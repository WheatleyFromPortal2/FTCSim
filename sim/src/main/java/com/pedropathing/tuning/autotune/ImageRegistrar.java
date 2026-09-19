package com.pedropathing.tuning.autotune;

import java.io.InputStream;
import java.util.concurrent.Callable;

/** FTCSim stand-in: images for the web tuner are registered but never served. */
public final class ImageRegistrar {
    private ImageRegistrar() {}

    public static Handle registerPNG(Callable<InputStream> data) { return new Handle(data, "image/png"); }
    public static Handle registerAssetPNG(String path) { return new Handle(() -> null, "image/png"); }
    public static Handle registerJPEG(Callable<InputStream> data) { return new Handle(data, "image/jpeg"); }
    public static Handle registerAssetJPEG(String path) { return new Handle(() -> null, "image/jpeg"); }
    public static Handle registerSVG(Callable<InputStream> data) { return new Handle(data, "image/svg+xml"); }
    public static Handle registerAssetSVG(String path) { return new Handle(() -> null, "image/svg+xml"); }

    public static final class Handle {
        public final String id = Utils.nanoid();
        public final Callable<InputStream> data;
        public final String mimeType;
        Handle(Callable<InputStream> data, String mimeType) { this.data = data; this.mimeType = mimeType; }
    }
}
