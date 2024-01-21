package com.arielfaridja.ezrahi.entities;

public enum ReportType {
    GENERAL(0),
    MEDICAL(1);

    private final int value;

    ReportType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ReportType getByValue(int value) {
        switch (value) {
            case 0:
                return GENERAL;
            case 1:
                return MEDICAL;
            default:
                return GENERAL;
        }
    }
}
