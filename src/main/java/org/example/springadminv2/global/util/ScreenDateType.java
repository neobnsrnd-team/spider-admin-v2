package org.example.springadminv2.global.util;

public enum ScreenDateType {
    HISTORY_AUDIT(30),
    BATCH_EXECUTION(0),
    TRANSACTION_TRACE(1),
    ERROR_LOG(7),
    SYSTEM_MONITOR(0);

    private final int daysBack;

    ScreenDateType(int daysBack) {
        this.daysBack = daysBack;
    }

    public int getDaysBack() {
        return daysBack;
    }
}
