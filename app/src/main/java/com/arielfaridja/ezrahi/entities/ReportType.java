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
}
