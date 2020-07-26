package com.kuaishou.domain;

import com.kuaishou.kcode.KcodeAlertAnalysisImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * @Description           服务级别信息
 * @author Gwjjj
 * @date 22:39 2020/7/25
 */
public class MonitorLogDomian {
    public List<MyMonitor> monitorList;
    public Map<Long, LogDomain[]> logMap;
    public AtomicInteger atomicInteger;                     // 多线程标志位
    public AtomicInteger[] trueC;                           // 服务级别成功调用数
    public AtomicInteger[] allC;                            // 服务级别总调用数
    public List<Integer>[] allList;                         //  服务级别耗时列表
    public List<String>[] p99String;                        // q2
    public List<String>[] spString;                         // q2
    public String[] sp;                                     // 分钟成功率
    public String[] p99;                                    // 分钟p99
    public MonitorLogDomian() {
        logMap = new ConcurrentHashMap<>();
        atomicInteger  = new AtomicInteger();
        trueC = new AtomicInteger[KcodeAlertAnalysisImpl.endTime];
        allC = new AtomicInteger[KcodeAlertAnalysisImpl.endTime];
        sp = new String[KcodeAlertAnalysisImpl.endTime];
        p99 = new String[KcodeAlertAnalysisImpl.endTime];
        for (int i = 0; i < KcodeAlertAnalysisImpl.endTime; i++) {
            trueC[i] = new AtomicInteger();
            allC[i] = new AtomicInteger();
            p99[i] = "";
        }
        monitorList = new ArrayList<>();
    }
}
