package it.edo.codecprobe;

/** One process-wide lease, held until the last decoder/encoder and output are closed. */
final class MediaConversionGate {
    static final ConversionRunGate INSTANCE=new ConversionRunGate();
    private MediaConversionGate(){}
}
