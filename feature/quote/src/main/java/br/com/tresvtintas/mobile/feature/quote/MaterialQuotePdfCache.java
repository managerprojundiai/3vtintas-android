package br.com.tresvtintas.mobile.feature.quote;

import android.content.Context;
import android.net.Uri;
import androidx.core.content.FileProvider;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteException;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteFailureKind;
import br.com.tresvtintas.mobile.core.quote.MaterialQuotePdfDownload;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteRepository;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;

public final class MaterialQuotePdfCache {
    private static final String ROOT_DIRECTORY = "material-quote-pdfs";
    private static final String PARTIAL_FILENAME = "document.part";
    private static final long RETENTION_MILLIS = 60L * 60L * 1000L;
    private static final int MAX_CACHED_DOCUMENTS = 8;

    private MaterialQuotePdfCache() {
    }

    public record Artifact(Uri uri, String filename, long bytes) {
        public Artifact {
            Objects.requireNonNull(uri, "PDF content URI is required.");
            new MaterialQuotePdfDownload(filename, bytes);
        }
    }

    public static Artifact download(
            Context context,
            MaterialQuoteRepository repository,
            long quoteId) throws MaterialQuoteException {
        Objects.requireNonNull(context, "Context is required.");
        Objects.requireNonNull(repository, "Quote repository is required.");
        File root = root(context);
        prepareRoot(root);
        prune(root, System.currentTimeMillis());
        File working = new File(root, UUID.randomUUID().toString());
        if (!working.mkdir()) {
            throw storageFailure("Temporary PDF directory could not be created.", null);
        }
        File partial = new File(working, PARTIAL_FILENAME);
        try {
            MaterialQuotePdfDownload download;
            try (OutputStream destination = Files.newOutputStream(
                    partial.toPath())) {
                download = repository.downloadPdf(quoteId, destination);
            }
            File completed = new File(working, download.filename());
            moveAtomically(partial, completed);
            working.setLastModified(System.currentTimeMillis());
            Uri uri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    completed);
            return new Artifact(uri, download.filename(), download.bytesWritten());
        } catch (MaterialQuoteException exception) {
            deleteTree(working);
            throw exception;
        } catch (IOException | IllegalArgumentException exception) {
            deleteTree(working);
            throw storageFailure(
                    "The quote PDF could not be finalized in private cache.",
                    exception);
        }
    }

    public static void clear(Context context) {
        if (context != null) {
            deleteTree(root(context));
        }
    }

    private static File root(Context context) {
        return new File(context.getCacheDir(), ROOT_DIRECTORY);
    }

    private static void prepareRoot(File root) throws MaterialQuoteException {
        if (Files.isSymbolicLink(root.toPath())
                || (!root.exists() && !root.mkdirs())
                || !root.isDirectory()) {
            throw storageFailure("Private PDF cache is unavailable.", null);
        }
    }

    private static void prune(File root, long now) throws MaterialQuoteException {
        File[] directories = root.listFiles(File::isDirectory);
        if (directories == null) {
            throw storageFailure("Private PDF cache cannot be inspected.", null);
        }
        Arrays.sort(
                directories,
                Comparator.comparingLong(File::lastModified).reversed());
        for (int index = 0; index < directories.length; index++) {
            File directory = directories[index];
            boolean expired = now - directory.lastModified() > RETENTION_MILLIS;
            boolean exceedsCapacity =
                    index >= MAX_CACHED_DOCUMENTS - 1;
            if ((expired || exceedsCapacity) && !deleteTree(directory)) {
                throw storageFailure("Expired PDF cache could not be removed.", null);
            }
        }
    }

    private static void moveAtomically(File source, File target) throws IOException {
        try {
            Files.move(
                    source.toPath(),
                    target.toPath(),
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source.toPath(), target.toPath());
        }
    }

    private static boolean deleteTree(File file) {
        if (!file.exists()) {
            return true;
        }
        try {
            if (!Files.isSymbolicLink(file.toPath()) && file.isDirectory()) {
                File[] children = file.listFiles();
                if (children == null) {
                    return false;
                }
                for (File child : children) {
                    if (!deleteTree(child)) {
                        return false;
                    }
                }
            }
            return file.delete();
        } catch (SecurityException exception) {
            return false;
        }
    }

    private static MaterialQuoteException storageFailure(
            String message,
            Throwable cause) {
        return new MaterialQuoteException(
                MaterialQuoteFailureKind.LOCAL_STORAGE,
                message,
                cause);
    }
}
