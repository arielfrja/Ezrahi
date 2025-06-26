package com.arielfaridja.ezrahi.entities;

public enum ReportStatus {
    REPORTED(1),
    HANDLED(2);

    private final int value;

    ReportStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ReportStatus getByValue(int value) {
        switch (value) {
            case 1:
                return REPORTED;
            case 2:
                return HANDLED;
            default:
                return REPORTED;
        }
    }
}
