package com.kuaishou.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class LogDomain {
    public AtomicInteger trueCount;
    public AtomicInteger allCount;
    public List<Integer> timeList;
    public int p99 = 0;
    public double successPercent = 1.00d;

    public LogDomain() {
        this.trueCount = new AtomicInteger(0);
        this.allCount = new AtomicInteger(0);
        this.timeList = Collections.synchronizedList(new ArrayList<>());
    }

    public void addTrueCount(){
        trueCount.incrementAndGet();
    }
    public void addAllCount(){
        allCount.incrementAndGet();
    }
    public void addTimeList(int addElement){
        timeList.add(addElement);
    }
}
