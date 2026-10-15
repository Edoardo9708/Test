package it.edo.codecprobe;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/** Cooperative worker gate. Commands never wait for encoding or perform file I/O. */
final class ConversionControl {
    private final AtomicBoolean cancelled;
    private final LongSupplier clock;
    private final Consumer<Boolean> pauseChanged;
    private boolean pauseRequested, paused, skipRequested, active;
    private int file;
    private long pauseStart, pausedTotal;

    ConversionControl(AtomicBoolean cancelled, LongSupplier clock, Consumer<Boolean> pauseChanged) {
        this.cancelled = cancelled; this.clock = clock; this.pauseChanged = pauseChanged;
    }

    synchronized void beginFile(int number) { file = number; active = true; skipRequested = false; }
    synchronized void endFile() { active = false; skipRequested = false; }
    synchronized boolean skip(int number) {
        if (!active || file != number || cancelled.get() || skipRequested) return false;
        skipRequested = true; notifyAll(); return true;
    }
    synchronized void pause(boolean value) { pauseRequested = value; notifyAll(); }
    synchronized void cancel() { cancelled.set(true); pauseRequested = false; notifyAll(); }
    synchronized boolean skipped() { return skipRequested; }
    synchronized boolean stopping() { return cancelled.get() || skipRequested; }
    synchronized boolean pauseRequested() { return pauseRequested; }
    synchronized long pausedMillis() { return pausedTotal + (paused ? Math.max(0, clock.getAsLong() - pauseStart) : 0); }

    /** Called only by the worker (also through codec callbacks). No polling/busy loop. */
    boolean checkpoint() {
        boolean entered;
        synchronized(this) {
            if(!pauseRequested || stopping())return stopping();
            entered = true;
            paused = true; pauseStart = clock.getAsLong();
        }
        try {
            if(entered)pauseChanged.accept(true);
            synchronized(this) {
                while(pauseRequested && !stopping()) {
                    try { wait(); }
                    catch(InterruptedException interrupted) {
                        cancelled.set(true); Thread.currentThread().interrupt(); return true;
                    }
                }
                return stopping();
            }
        } finally {
            if(entered) {
                synchronized(this) {
                    pausedTotal += Math.max(0, clock.getAsLong() - pauseStart);
                    paused = false;
                }
                pauseChanged.accept(false);
            }
        }
    }

    /** Atomic boundary: after acceptance closes, a late Skip cannot discard a completed file. */
    boolean finishFile() {
        while(true) {
            if(checkpoint())return false;
            synchronized(this) {
                if(stopping())return false;
                if(pauseRequested)continue;
                active = false;
                return true;
            }
        }
    }
}
