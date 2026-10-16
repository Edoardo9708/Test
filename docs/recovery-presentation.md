# Recovery presentation (v0.56)

Cold process recovery keeps the initial selection empty until explicit resume.
The conversion dashboard shows zero percent, no current file and placeholders
for frames, video seconds and file parameters while a saved session is waiting.
Queue counts, completed-file report, errors, output links and accumulated elapsed
time remain visible. The durable checkpoint itself is retained unchanged; once
encoding starts, the existing per-file reset and actual progress take over.

After the user confirms recovery, grants permissions and completes output-name
preflight, launching the saved request invokes `HomeRecoveredSelection`. It copies
the queue's names, URIs and per-file/global destination rules into the editable
selection without changing the already submitted conversion request.

Missing metadata are read by the existing single selection executor, one file at
a time. Recovery uses a Toast and no modal metadata dialog. Cached entries are
reused, completed details become visible, and carousel navigation remains usable
while loading. Conversion/saving controls stay available during this background
inspection. The reader does not start another conversion worker.

A recovered-selection marker survives Activity recreation in the same process,
so incomplete metadata loading resumes in background mode. Process-death Bundle
filtering removes that marker together with the selection, preserving explicit
recovery on the next cold launch. Generation checks invalidate stale callbacks,
and Activity destruction shuts down its existing inspection executor.

Normal new selection retains its existing modal loading flow. Repeated dashboard
updates do not import selections or trigger metadata reads. The selected request
is independent of metadata display names that may be refined asynchronously.

## Explicit save and stop (v0.57)

Once the worker has returned from converter cleanup, a recoverable Save and stop
uses `ConversionSessionState.clearInterruptedFile()` before its final checkpoint.
It clears percent, frame/video-time counters, effective encoder metrics and
memory-protection status in both live state and the saved session. Queue items,
output links, errors, requested settings and accumulated time remain intact.
Resetting after cleanup prevents final encoder callbacks from restoring stale
values. Ordinary in-memory pause retains its counters and never calls this reset.
Abrupt process death can still leave a historical checkpoint; the existing
recovery presentation hides those per-file metrics until conversion starts.
