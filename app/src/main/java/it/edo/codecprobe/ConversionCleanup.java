package it.edo.codecprobe;

import java.io.File;
import java.io.IOException;

/** Cleanup failures must remain visible, especially when skipping a file. */
final class ConversionCleanup {
    static final class Failed extends IOException {
        Failed(String message, Throwable cause) { super(message, cause); }
    }
    static void temporaryFiles(File... files) throws Failed {
        Failed problem = null;
        for(File file : files) {
            if(file != null && file.exists() && !file.delete()) {
                Failed error = new Failed("Impossibile eliminare il temporaneo: " + file.getName(), null);
                if(problem == null)problem = error;else problem.addSuppressed(error);
            }
        }
        if(problem != null)throw problem;
    }
    private ConversionCleanup() {}
}
