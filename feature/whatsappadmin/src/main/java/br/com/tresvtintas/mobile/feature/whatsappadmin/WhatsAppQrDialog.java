package br.com.tresvtintas.mobile.feature.whatsappadmin;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationModels.EphemeralQr;
import br.com.tresvtintas.mobile.feature.whatsappadmin.databinding.WhatsAppAdminQrDialogBinding;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

final class WhatsAppQrDialog {
    private static final int MAXIMUM_BITMAP_DIMENSION = 1_024;
    private final Activity activity;
    private final Executor worker;
    private final Executor main;
    private final AtomicLong generation = new AtomicLong();
    private Optional<AlertDialog> dialog = Optional.empty();
    private Optional<Bitmap> displayedBitmap = Optional.empty();

    WhatsAppQrDialog(Activity activity, Executor worker, Executor main) {
        this.activity = Objects.requireNonNull(activity, "Activity is required.");
        this.worker = Objects.requireNonNull(worker, "Worker is required.");
        this.main = Objects.requireNonNull(main, "Main executor is required.");
    }

    void show(EphemeralQr qr) {
        dismiss();
        long requestGeneration = generation.incrementAndGet();
        WhatsAppAdminQrDialogBinding binding =
                WhatsAppAdminQrDialogBinding.inflate(LayoutInflater.from(activity));
        binding.pairingCode.setText(qr.pairingCode().orElse(""));
        binding.pairingContainer.setVisibility(
                qr.pairingCode().isPresent() ? View.VISIBLE : View.GONE);
        binding.qrImage.setVisibility(View.GONE);
        binding.progress.setVisibility(
                qr.encodedImage().isPresent() ? View.VISIBLE : View.GONE);
        AlertDialog created = new AlertDialog.Builder(activity)
                .setTitle(R.string.whatsapp_admin_qr_title)
                .setMessage(R.string.whatsapp_admin_qr_message)
                .setView(binding.getRoot())
                .setPositiveButton(R.string.whatsapp_admin_close, null)
                .create();
        created.setOnDismissListener(ignored -> onDismiss(created));
        dialog = Optional.of(created);
        created.show();
        qr.encodedImage().ifPresent(value -> worker.execute(() ->
                decodeAndRender(value, binding, requestGeneration)));
    }

    void dismiss() {
        generation.incrementAndGet();
        Optional<AlertDialog> previous = dialog;
        dialog = Optional.empty();
        previous.ifPresent(AlertDialog::dismiss);
        clearDisplayedBitmap();
    }

    private void decodeAndRender(
            String encoded,
            WhatsAppAdminQrDialogBinding binding,
            long requestGeneration) {
        try {
            byte[] bytes = WhatsAppQrPayload.decode(encoded);
            Bitmap bitmap = decodeBitmap(bytes);
            main.execute(() -> renderBitmap(
                    binding,
                    bitmap,
                    requestGeneration));
        } catch (IllegalArgumentException failure) {
            main.execute(() -> renderInvalid(binding, requestGeneration));
        }
    }

    private void renderBitmap(
            WhatsAppAdminQrDialogBinding binding,
            Bitmap bitmap,
            long requestGeneration) {
        if (!active(requestGeneration)) {
            bitmap.recycle();
            return;
        }
        clearDisplayedBitmap();
        displayedBitmap = Optional.of(bitmap);
        binding.progress.setVisibility(View.GONE);
        binding.qrImage.setImageBitmap(bitmap);
        binding.qrImage.setVisibility(View.VISIBLE);
    }

    private void renderInvalid(
            WhatsAppAdminQrDialogBinding binding,
            long requestGeneration) {
        if (!active(requestGeneration)) {
            return;
        }
        binding.progress.setVisibility(View.GONE);
        binding.qrFailure.setVisibility(View.VISIBLE);
    }

    private boolean active(long requestGeneration) {
        return generation.get() == requestGeneration
                && dialog.filter(AlertDialog::isShowing).isPresent()
                && !activity.isFinishing()
                && !activity.isDestroyed();
    }

    private void onDismiss(AlertDialog dismissed) {
        if (dialog.filter(value -> value == dismissed).isPresent()) {
            generation.incrementAndGet();
            dialog = Optional.empty();
            clearDisplayedBitmap();
        }
    }

    private void clearDisplayedBitmap() {
        displayedBitmap.ifPresent(bitmap -> {
            if (!bitmap.isRecycled()) {
                bitmap.recycle();
            }
        });
        displayedBitmap = Optional.empty();
    }

    private static Bitmap decodeBitmap(byte[] bytes) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
        if (bounds.outWidth < 1 || bounds.outHeight < 1) {
            throw new IllegalArgumentException("QR bitmap is invalid.");
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight);
        Bitmap bitmap = BitmapFactory.decodeByteArray(
                bytes,
                0,
                bytes.length,
                options);
        if (bitmap == null) {
            throw new IllegalArgumentException("QR bitmap is invalid.");
        }
        return bitmap;
    }

    private static int sampleSize(int width, int height) {
        int result = 1;
        while (width / result > MAXIMUM_BITMAP_DIMENSION
                || height / result > MAXIMUM_BITMAP_DIMENSION) {
            result *= 2;
        }
        return result;
    }
}
