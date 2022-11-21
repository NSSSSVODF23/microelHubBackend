package com.microel.microelhub.storage.proxies;

import java.sql.Timestamp;

public interface RDStatisticPoint {
    Timestamp getDay();
    Integer getOId();
    Integer getSId();
    Integer getMId();
    Integer getLId();
    Integer getXId();
    Integer getOD();
    Integer getSD();
    Integer getMD();
    Integer getLD();
    Integer getXD();
    Integer getA();
    Float getAvgDelay();
    Float getAvgDuration();
}
