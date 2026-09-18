package android.os;

/** Desktop stand-in for android.os.Build (reports an API 28 device like the REV Control Hub). */
public final class Build {
    private Build() {}
    public static final String MODEL = "FTCSim";
    public static final String MANUFACTURER = "FTCSim";
    public static final String BRAND = "FTCSim";
    public static final String DEVICE = "ftcsim";
    public static final String PRODUCT = "ftcsim";
    public static final String HARDWARE = "desktop";
    public static final String SERIAL = "FTCSIM";
    public static final String ID = "FTCSIM";
    public static final String DISPLAY = "FTCSim desktop";
    public static final String FINGERPRINT = "ftcsim/desktop";
    public static final String[] SUPPORTED_ABIS = { System.getProperty("os.arch", "x86_64") };

    public static final class VERSION {
        private VERSION() {}
        public static final int SDK_INT = 28;
        public static final String RELEASE = "9";
        public static final String SDK = "28";
        public static final String CODENAME = "REL";
        public static final String INCREMENTAL = "1";
    }

    public static final class VERSION_CODES {
        private VERSION_CODES() {}
        public static final int BASE = 1, BASE_1_1 = 2, CUPCAKE = 3, DONUT = 4, ECLAIR = 5, ECLAIR_0_1 = 6,
            ECLAIR_MR1 = 7, FROYO = 8, GINGERBREAD = 9, GINGERBREAD_MR1 = 10, HONEYCOMB = 11, HONEYCOMB_MR1 = 12,
            HONEYCOMB_MR2 = 13, ICE_CREAM_SANDWICH = 14, ICE_CREAM_SANDWICH_MR1 = 15, JELLY_BEAN = 16,
            JELLY_BEAN_MR1 = 17, JELLY_BEAN_MR2 = 18, KITKAT = 19, KITKAT_WATCH = 20, LOLLIPOP = 21,
            LOLLIPOP_MR1 = 22, M = 23, N = 24, N_MR1 = 25, O = 26, O_MR1 = 27, P = 28, Q = 29, R = 30,
            S = 31, S_V2 = 32, TIRAMISU = 33, UPSIDE_DOWN_CAKE = 34, VANILLA_ICE_CREAM = 35, CUR_DEVELOPMENT = 10000;
    }
}
