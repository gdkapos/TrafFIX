package com.example.traffix;

import java.math.BigDecimal;
import java.util.Date;

public class Dieleusi {
    private long junction;
    private long time;
    private int possibility;
    private int direction;

    public Dieleusi(Long junction, long time, int possibility, int direction) {
        this.junction = junction;
        this.time = time;
        this.possibility = possibility;
        this.direction = direction;
    }

    public long getJunction() {
        return junction;
    }

    public long getTime() {
        return time;
    }

    public int getPossibility() {
        return possibility;
    }

    public int getDirection() {
        return direction;
    }
}
