package br.com.tresvtintas.mobile.feature.laborquote;

import android.content.Context;
import android.net.Uri;
import androidx.core.content.FileProvider;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteException;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteFailureKind;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuotePdfDownload;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteRepository;
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

public final class LaborQuotePdfCache {
    private static final String ROOT_DIRECTORY = "labor-quote-pdfs";
    private static final String PARTIAL_FILENAME = "document.part";
    private static final long RETENTION_MILLIS = 60L * 60L * 1000L;
    private static final int MAXIMUM_DOCUMENTS = 8;

    private LaborQuotePdfCache() {
    }

    public record Artifact(Uri uri, String filename, long bytes) {
        public Artifact {
            Objects.requireNonNull(uri, "Labor quote PDF URI is required.");
            new LaborQuotePdfDownload(filename, bytes);
        }
    }

    public static Artifact download(
            Context context,
            LaborQuoteRepository repository,
            long quoteId) throws LaborQuoteException {
        Objects.requireNonNull(context, "Context is required.");
        Objects.requireNonNull(repository, "Labor quote repository is required.");
        File root = root(context);
        prepareRoot(root);
        prune(root, System.currentTimeMillis());
        File working = new File(root, UUID.randomUUID().toString());
        if (!working.mkdir()) {
            throw storage("Temporary PDF directory could not be created.", null);
        }
        File partial = new File(working, PARTIAL_FILENAME);
        try {
            LaborQuotePdfDownload download;
            try (OutputStream destination = Files.newOutputStream(partial.toPath())) {
                download = repository.downloadPdf(quoteId, destination);
            }
            File completed = new File(working, download.filename());
            move(partial, completed);
            working.setLastModified(System.currentTimeMillis());
            Uri uri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    completed);
            return new Artifact(uri, download.filename(), download.bytesWritten());
        } catch (LaborQuoteException exception) {
            deleteTree(working);
            throw exception;
        } catch (IOException | IllegalArgumentException exception) {
            deleteTree(working);
            throw storage("Labor quote PDF cannot be finalized in private cache.", exception);
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

    private static void prepareRoot(File root) throws LaborQuoteException {
        if (Files.isSymbolicLink(root.toPath())
                || (!root.exists() && !root.mkdirs())
                || !root.isDirectory()) {
            throw storage("Private labor quote PDF cache is unavailable.", null);
        }
    }

    private static void prune(File root, long now) throws LaborQuoteException {
        File[] directories = root.listFiles(File::isDirectory);
        if (directories == null) {
            throw storage("Private labor quote PDF cache cannot be inspected.", null);
        }
        Arrays.sort(directories, Comparator.comparingLong(File::lastModified).reversed());
        for (int index = 0; index < directories.length; index++) {
            boolean expired = now - directories[index].lastModified() > RETENTION_MILLIS;
            boolean excess = index >= MAXIMUM_DOCUMENTS - 1;
            if ((expired || excess) && !deleteTree(directories[index])) {
                throw storage("Expired labor quote PDF cannot be removed.", null);
            }
        }
    }

    private static void move(File source, File target) throws IOException {
        try {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE);
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

    private static LaborQuoteException storage(String message, Throwable cause) {
        return new LaborQuoteException(LaborQuoteFailureKind.LOCAL_STORAGE, message, cause);
    }
}
