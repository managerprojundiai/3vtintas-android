package br.com.tresvtintas.mobile.feature.laborquote;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.IntentCompat;
import br.com.tresvtintas.mobile.feature.laborquote.databinding.LaborQuoteActivityPdfBinding;
import java.io.IOException;
import java.util.regex.Pattern;

public final class LaborQuotePdfViewerActivity extends AppCompatActivity {
    private static final String EXTRA_URI = "br.com.tresvtintas.mobile.laborquote.PDF_URI";
    private static final String EXTRA_FILENAME =
            "br.com.tresvtintas.mobile.laborquote.PDF_FILENAME";
    private static final String STATE_PAGE = "labor_quote_pdf_page";
    private static final int MAXIMUM_WIDTH = 2400;
    private static final long MAXIMUM_PIXELS = 12_000_000L;
    private static final Pattern SAFE_FILENAME = Pattern.compile(
            "orcamento-[0-9]{6,15}-labor\\.pdf");
    private LaborQuoteActivityPdfBinding binding;
    private Uri documentUri;
    private String filename;
    private ParcelFileDescriptor descriptor;
    private PdfRenderer renderer;
    private Bitmap bitmap;
    private int pageIndex;
    private boolean closed;

    public static Intent intent(Context context, LaborQuotePdfCache.Artifact artifact) {
        return new Intent(context, LaborQuotePdfViewerActivity.class)
                .putExtra(EXTRA_URI, artifact.uri())
                .putExtra(EXTRA_FILENAME, artifact.filename());
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        LaborQuotePrivacy.protect(this);
        EdgeToEdge.enable(this);
        binding = LaborQuoteActivityPdfBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.laborQuotePdfToolbar.setNavigationOnClickListener(ignored -> finish());
        binding.laborQuotePdfPrevious.setOnClickListener(ignored -> showPage(pageIndex - 1));
        binding.laborQuotePdfNext.setOnClickListener(ignored -> showPage(pageIndex + 1));
        binding.laborQuotePdfShare.setOnClickListener(ignored -> share());
        pageIndex = state == null ? 0 : state.getInt(STATE_PAGE, 0);
        documentUri = IntentCompat.getParcelableExtra(getIntent(), EXTRA_URI, Uri.class);
        filename = getIntent().getStringExtra(EXTRA_FILENAME);
        if (!validInput()) {
            fail();
            return;
        }
        open();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putInt(STATE_PAGE, pageIndex);
        super.onSaveInstanceState(state);
    }

    @Override
    protected void onDestroy() {
        closeDocument();
        super.onDestroy();
    }

    private boolean validInput() {
        return documentUri != null
                && "content".equals(documentUri.getScheme())
                && (getPackageName() + ".fileprovider").equals(documentUri.getAuthority())
                && filename != null
                && SAFE_FILENAME.matcher(filename).matches();
    }

    private void open() {
        try {
            descriptor = getContentResolver().openFileDescriptor(documentUri, "r");
            if (descriptor == null) {
                fail();
                return;
            }
            renderer = new PdfRenderer(descriptor);
            if (renderer.getPageCount() == 0) {
                fail();
                return;
            }
            pageIndex = Math.min(pageIndex, renderer.getPageCount() - 1);
            binding.laborQuotePdfImage.post(() -> showPage(pageIndex));
        } catch (IOException | SecurityException exception) {
            fail();
        }
    }

    private void showPage(int requested) {
        if (closed || renderer == null || requested < 0 || requested >= renderer.getPageCount()) {
            return;
        }
        binding.laborQuotePdfProgress.setVisibility(View.VISIBLE);
        try (PdfRenderer.Page page = renderer.openPage(requested)) {
            int width = Math.min(
                    MAXIMUM_WIDTH,
                    Math.max(1, Math.max(binding.laborQuotePdfImage.getWidth(), page.getWidth()) * 2));
            double scale = (double) width / page.getWidth();
            int height = Math.max(1, (int) Math.ceil(page.getHeight() * scale));
            if ((long) width * height > MAXIMUM_PIXELS) {
                scale = Math.sqrt((double) MAXIMUM_PIXELS
                        / ((long) page.getWidth() * page.getHeight()));
                width = Math.max(1, (int) Math.floor(page.getWidth() * scale));
                height = Math.max(1, (int) Math.floor(page.getHeight() * scale));
            }
            Bitmap next = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            page.render(next, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            recycleBitmap();
            bitmap = next;
            pageIndex = requested;
            binding.laborQuotePdfImage.setImageBitmap(bitmap);
            binding.laborQuotePdfPage.setText(getString(
                    R.string.labor_quote_pdf_page,
                    pageIndex + 1,
                    renderer.getPageCount()));
            binding.laborQuotePdfPrevious.setEnabled(pageIndex > 0);
            binding.laborQuotePdfNext.setEnabled(pageIndex + 1 < renderer.getPageCount());
            binding.laborQuotePdfScroll.scrollTo(0, 0);
            binding.laborQuotePdfError.setText("");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            fail();
        } finally {
            binding.laborQuotePdfProgress.setVisibility(View.INVISIBLE);
        }
    }

    private void share() {
        if (!validInput()) {
            return;
        }
        Intent send = new Intent(Intent.ACTION_SEND)
                .setType("application/pdf")
                .putExtra(Intent.EXTRA_STREAM, documentUri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        send.setClipData(ClipData.newRawUri(filename, documentUri));
        Intent chooser = Intent.createChooser(send, getString(
                R.string.labor_quote_pdf_share_chooser));
        chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(chooser);
        } catch (ActivityNotFoundException exception) {
            binding.laborQuotePdfError.setText(R.string.labor_quote_pdf_share_unavailable);
        }
    }

    private void fail() {
        closeDocument();
        binding.laborQuotePdfProgress.setVisibility(View.INVISIBLE);
        binding.laborQuotePdfImage.setVisibility(View.GONE);
        binding.laborQuotePdfPrevious.setEnabled(false);
        binding.laborQuotePdfNext.setEnabled(false);
        binding.laborQuotePdfShare.setEnabled(false);
        binding.laborQuotePdfError.setText(R.string.labor_quote_pdf_failure);
    }

    private void closeDocument() {
        if (closed) {
            return;
        }
        closed = true;
        recycleBitmap();
        if (renderer != null) {
            renderer.close();
        }
        if (descriptor != null) {
            try {
                descriptor.close();
            } catch (IOException ignored) {
                // The session-owned private cache is cleared when scope is revoked.
            }
        }
    }

    private void recycleBitmap() {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }
}
