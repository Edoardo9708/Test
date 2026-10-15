package it.edo.codecprobe;

import java.io.PrintWriter;
import java.io.StringWriter;

/** Immutable failure captured on the worker, independent of log tail truncation. */
final class ConversionFailure {
    final int number;
    final String name, message, lastState, details;

    ConversionFailure(int number, String name, String lastState, Throwable error) {
        this.number = number;
        this.name = name;
        this.lastState = lastState;
        String text = error.getMessage();
        message = text == null || text.trim().isEmpty() ? error.getClass().getSimpleName() : text;
        StringWriter stack = new StringWriter();
        error.printStackTrace(new PrintWriter(stack));
        details = stack.toString();
    }

    String title() {
        return "Video " + number + " · " + name.replace('\n', ' ').replace('\r', ' ');
    }

    String copyText() {
        return "ERRORE CONVERSIONE · v" + BuildConfig.VERSION_NAME + "\n" + title()
            + "\n\nMESSAGGIO\n" + message + "\n\nULTIMO STATO COMUNICATO\n" + lastState
            + "\n\nDETTAGLI TECNICI\n" + details;
    }
}
