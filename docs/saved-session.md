# Durable queue and per-file result actions

The simple recovery mode restarts an interrupted video from the beginning. It does not
serialize x265, decoder, muxer, or native memory. Complete, failed, omitted, explicitly
skipped, and processed-but-not-saved entries are terminal and are not processed again.

`SavedSession` owns the versioned JSON schema in the app's files directory. Android
`AtomicFile` replaces the checkpoint atomically and syncs its stream. The journal contains
the frozen request, approved output names, directory choices, one status/size/output URI
per input, complete structured errors, current progress/encoder metrics, and accumulated
elapsed time. The log itself remains a separate file. No source video bytes are duplicated.

The service saves before processing, at macro steps and terminal file outcomes, and at
most once per ten seconds for ordinary progress messages. Disk writes happen on its
worker. A journal write failure stops processing and leaves the previous valid checkpoint.
After unexpected process death, the last checkpoint remains recoverable. Foreground
service timeout also requests a durable stop. Recovery never starts automatically.

`Salva e interrompi` cancels the current file cooperatively, runs the existing cleanup,
saves the queue/report, releases the wake lock and stops the foreground service. The
existing temporary pause still keeps codec state in RAM. Normal cancellation closes
the queue; it is distinct from durable stop. The final report is persisted even for a
finished queue, and `Svuota tutto` removes both in-memory and durable session data without
deleting completed media or logs. A new queue requires confirmation before replacing a
recoverable one.

`HomeSavedSession` owns recovery/stop/replacement dialogs. `HomeOutputCheck` checks only
unfinished destinations on recovery: completed files do not trigger rename prompts.
An output left during a sudden death is never assumed complete or silently overwritten;
if its planned name exists, the normal rename/ignore dialog handles it before encoding.
The original batch log is appended on recovery, preserving earlier diagnostics.

`HomeFailureReport` renders an `Apri` action for each successful entry, bound to its exact
URI and input number rather than filename matching alone. These links survive process
restart because the journal persists their index-to-URI mapping. The `Report lavori` heading identifies the report, with no aggregate result-opening button.

Tests cover journal round trips, failure stacks, interrupted atomic writes, invalid data,
service stop/recreation/recovery, terminal-entry skipping, append-only recovery logging,
fresh-Activity recovery controls, replacement confirmation, exact per-file opening, and
retry isolation. Native encoding algorithms are unchanged.

## Recovery lifecycle corrections

`SessionClock` is the sole elapsed-time formula used by dashboard rendering, the one-second
system monitor, the journal, and end-of-queue logging. Accumulated time survives recovery;
the interval while the app is stopped is excluded. The monitor cancels its prior callback
before restarting and does not reschedule after the screen becomes inactive.

The home selection is independent of the saved queue/report. Reopening does not import
the queue into the editable selection or trigger metadata scans. A process identifier in
the Activity Bundle distinguishes same-process recreation from process death; a Bundle
from an old process keeps UI preferences but discards selection and stale folder actions.
Same-process recreation still preserves the current selection and metadata cache.

`ConversionSessionState` defines reset boundaries. Every run gets a fresh State object;
recovery copies totals, results, errors, and accumulated time. Per-file progress and all
five effective encoder metrics reset before a restarted/next file. Run-control flags reset
at queue start. `ConversionRunGate` allows only one conversion worker across service
instances, even after onDestroy while cleanup is still running. Ownership is released only
after the worker's converter calls and cleanup return. Service stop uses the latest start
identifier; a stopping service instance refuses reuse rather than accepting work it may
cancel. A queued UI refresh is coalesced and checks State identity before updating anything.

`ConversionResources` samples the current PID, OS thread count, process PSS, Java heap,
C/C++ runtime heap, system available RAM and lowMemory on the existing worker. It writes
at most once per minute during work and after each file releases its resources. These
measurements help distinguish encoder memory demand from resources accumulating across
files; they do not establish why the user's phone consumed 3 GB or disconnected Bluetooth.
Codec parameters, CRF, lookahead and CPU settings are unchanged.
