package it.edo.codecprobe;

/** JPEG inventories only: bounded metadata payloads, never decoded pixels or source files. */
final class ImageMetadataCache {
    static final MetadataMemoryCache<Entry> memory = new MetadataMemoryCache<>(100, 24L * 1024 * 1024);
    static final class Entry {
        final String name, error;
        final JpegInventory.Info info;
        Entry(String name, JpegInventory.Info info, String error) { this.name = name; this.info = info; this.error = error; }
        long weight() { return 4096L + (name.length() + error.length()) * 2L + (info == null ? 0 : info.metadataBytes() + info.blocks.size() * 256L); }
    }
    private ImageMetadataCache() {}
}
