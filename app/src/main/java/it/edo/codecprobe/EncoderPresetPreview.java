package it.edo.codecprobe;

/**
* Native preset preview shared by the UI; never opens an encoder.
*/
final class EncoderPresetPreview {

    private static native int[] nativePresetSettings(int preset, boolean bframes, boolean lookahead);

    static int[] read(int preset, boolean bframes, boolean lookahead) {
        System.loadLibrary("x265probe");
        int[] values = nativePresetSettings(preset, bframes, lookahead);
        if (values == null || values.length != 5) throw new IllegalStateException("Parametri del preset incompleti");
        return values;
    }

    private EncoderPresetPreview() {
    }
}
