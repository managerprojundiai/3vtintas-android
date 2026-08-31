package br.com.tresvtintas.mobile.feature.quote;

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
import br.com.tresvtintas.mobile.feature.quote.databinding.QuoteActivityPdfBinding;
import java.io.IOException;
import java.util.regex.Pattern;

public final class MaterialQuotePdfViewerActivity extends AppCompatActivity {
    private static final String EXTRA_URI =
            "br.com.tresvtintas.mobile.quote.PDF_URI";
    private static final String EXTRA_FILENAME =
            "br.com.tresvtintas.mobile.quote.PDF_FILENAME";
    private static final String STATE_PAGE = "quote_pdf_page";
    private static final int MAX_RENDER_WIDTH = 2400;
    private static final long MAX_RENDER_PIXELS = 12_000_000L;
    private static final Pattern SAFE_FILENAME = Pattern.compile(
            "orcamento-[0-9]{6,15}-material\\.pdf");
    private QuoteActivityPdfBinding binding;
    private Uri documentUri;
    private String filename;
    private ParcelFileDescriptor descriptor;
    private PdfRenderer renderer;
    private Bitmap bitmap;
    private int pageIndex;
    private boolean documentClosed;

    public static Intent intent(
            Context context,
            MaterialQuotePdfCache.Artifact artifact) {
        return new Intent(context, MaterialQuotePdfViewerActivity.class)
                .putExtra(EXTRA_URI, artifact.uri())
                .putExtra(EXTRA_FILENAME, artifact.filename());
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        MaterialQuotePrivacy.protect(this);
        EdgeToEdge.enable(this);
        binding = QuoteActivityPdfBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.quotePdfToolbar.setNavigationOnClickListener(ignored -> finish());
        binding.quotePdfPrevious.setOnClickListener(
                ignored -> showPage(pageIndex - 1));
        binding.quotePdfNext.setOnClickListener(
                ignored -> showPage(pageIndex + 1));
        binding.quotePdfShare.setOnClickListener(ignored -> share());
        pageIndex = state == null ? 0 : state.getInt(STATE_PAGE, 0);
        documentUri = IntentCompat.getParcelableExtra(
                getIntent(), EXTRA_URI, Uri.class);
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
                && (getPackageName() + ".fileprovider")
                        .equals(documentUri.getAuthority())
                && filename != null
                && SAFE_FILENAME.matcher(filename).matches();
    }

    private void open() {
        try {
            descriptor = getContentResolver().openFileDescriptor(
                    documentUri, "r");
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
            binding.quotePdfImage.post(() -> showPage(pageIndex));
        } catch (IOException | SecurityException exception) {
            fail();
        }
    }

    private void showPage(int requestedPage) {
        if (documentClosed
                || renderer == null
                || requestedPage < 0
                || requestedPage >= renderer.getPageCount()) {
            return;
        }
        binding.quotePdfProgress.setVisibility(View.VISIBLE);
        try (PdfRenderer.Page page = renderer.openPage(requestedPage)) {
            int viewWidth = Math.max(
                    binding.quotePdfImage.getWidth(), page.getWidth());
            int width = Math.min(
                    MAX_RENDER_WIDTH, Math.max(1, viewWidth * 2));
            double scale = (double) width / page.getWidth();
            int height = Math.max(
                    1, (int) Math.ceil(page.getHeight() * scale));
            if ((long) width * height > MAX_RENDER_PIXELS) {
                scale = Math.sqrt(
                        (double) MAX_RENDER_PIXELS
                                / ((long) page.getWidth() * page.getHeight()));
                width = Math.max(
                        1, (int) Math.floor(page.getWidth() * scale));
                height = Math.max(
                        1, (int) Math.floor(page.getHeight() * scale));
            }
            Bitmap next = Bitmap.createBitmap(
                    width, height, Bitmap.Config.ARGB_8888);
            page.render(
                    next,
                    null,
                    null,
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            recycleBitmap();
            bitmap = next;
            pageIndex = requestedPage;
            binding.quotePdfImage.setImageBitmap(bitmap);
            binding.quotePdfPage.setText(getString(
                    R.string.quote_pdf_page,
                    pageIndex + 1,
                    renderer.getPageCount()));
            binding.quotePdfPrevious.setEnabled(pageIndex > 0);
            binding.quotePdfNext.setEnabled(
                    pageIndex + 1 < renderer.getPageCount());
            binding.quotePdfScroll.scrollTo(0, 0);
            binding.quotePdfError.setText("");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            fail();
        } finally {
            binding.quotePdfProgress.setVisibility(View.INVISIBLE);
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
        Intent chooser = Intent.createChooser(
                send,
                getString(R.string.quote_pdf_share_chooser));
        chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(chooser);
        } catch (ActivityNotFoundException exception) {
            binding.quotePdfError.setText(
                    R.string.quote_pdf_share_unavailable);
        }
    }

    private void fail() {
        closeDocument();
        binding.quotePdfProgress.setVisibility(View.INVISIBLE);
        binding.quotePdfImage.setVisibility(View.GONE);
        binding.quotePdfPrevious.setEnabled(false);
        binding.quotePdfNext.setEnabled(false);
        binding.quotePdfShare.setEnabled(false);
        binding.quotePdfError.setText(R.string.quote_pdf_failure);
    }

    private void closeDocument() {
        if (documentClosed) {
            return;
        }
        documentClosed = true;
        recycleBitmap();
        if (renderer != null) {
            renderer.close();
        }
        if (descriptor != null) {
            try {
                descriptor.close();
            } catch (IOException ignored) {
                // O descritor já está fora de uso e o cache será limpo pela sessão.
            }
        }
    }

    private void recycleBitmap() {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }
}
