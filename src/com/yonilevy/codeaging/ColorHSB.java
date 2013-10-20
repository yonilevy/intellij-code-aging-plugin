package com.yonilevy.codeaging;

public class ColorHSB {

    private final float hue;
    private final float saturation;
    private final float brightness;

    public ColorHSB(float hue, float saturation, float brightness) {
        this.hue = hue;
        this.saturation = saturation;
        this.brightness = brightness;
    }

    public float getBrightness() {
        return brightness;
    }

    public float getHue() {
        return hue;
    }

    public float getSaturation() {
        return saturation;
    }
}
