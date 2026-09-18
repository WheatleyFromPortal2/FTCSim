package android.graphics;

public class Matrix {
    public Matrix() {}
    public void reset() {}
    public boolean postRotate(float d) { return true; }
    public boolean postScale(float sx, float sy) { return true; }
    public boolean postTranslate(float dx, float dy) { return true; }
    public void setValues(float[] v) {}
    public void getValues(float[] v) {}
}
