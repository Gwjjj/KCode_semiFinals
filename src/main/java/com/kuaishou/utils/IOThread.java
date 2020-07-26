package com.kuaishou.utils;


import com.kuaishou.domain.LogDomain;
import com.kuaishou.domain.MonitorLogDomian;
import com.kuaishou.domain.MyMonitor;
import com.kuaishou.domain.MyPoint;
import com.kuaishou.kcode.KcodeAlertAnalysisImpl;

import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


/*/**
 * @Description     io线程池
 * @author Gwjjj
 * @date 15:43 2020/7/25
 */
public class IOThread implements Runnable {
    public static final byte SPACE_LINE = 10;                           //  \n 文件空行
    public static final byte COMMA = 44;                                //  逗号
    public static final byte POINT = 46;                                //  ip中的点
    public static final byte ZERO = 48;                                 //  0

    public IOThread(MappedByteBuffer mappedByteBuffer, int eachSize) {
        this.mappedByteBuffer = mappedByteBuffer;
        this.eachSize = eachSize;                                       //  此线程须读文件大小(因为不可能是正好分到空行，前面需找好最后一行)
    }


    @Override
    public void run() {
        long callIp;                                                    // 主调ip
        long calledIp;                                                  // 被调ip
        boolean isTrue;                                                 // 是否成功
        int time;                                                       // 耗时
        int callTime;                                                   // 调用时间
        long ips;                                                       // ip对
        MonitorLogDomian monitorLogDomian = null;
        LogDomain logDomains[];
        LogDomain logDomain;
        MyPoint callPoint;                                              //  主调在图中的点
        MyPoint calledPoint;                                            //  被调在图中的点
        while (offset != eachSize){
            callName = readString();
            callIp = readIp();
            calledName = readString();
            calledIp = readIp();
            isTrue = isTrue();
            time =  readInt ();
            callTime = readTime();
            if(!callName.equals(preCallName) || !calledName.equals(preCalledName)){
                monitorLogDomian = KcodeAlertAnalysisImpl.countMap.
                        computeIfAbsent(callName,k->new ConcurrentHashMap<>()).
                        computeIfAbsent(calledName,k->new MonitorLogDomian());
                // 只能一个线程修改
                if(monitorLogDomian.atomicInteger.compareAndSet(0,1)){
                    callPoint = KcodeAlertAnalysisImpl.graphMap.computeIfAbsent(callName,k -> new MyPoint(callName));
                    calledPoint = KcodeAlertAnalysisImpl.graphMap.computeIfAbsent(calledName,k -> new MyPoint(calledName));
                    callPoint.to.add(calledPoint);
                    calledPoint.from.add(callPoint);
                    if ((monitors = KcodeAlertAnalysisImpl.callMonitorMap.get(callName)) != null) {
                        for (int i = 0; i < monitors.size(); i++) {
                            monitorLogDomian.monitorList.add(monitors.get(i));
                        }
                    }
                    if ((monitors = KcodeAlertAnalysisImpl.calledMonitorMap.get(calledName)) != null) {
                        for (int i = 0; i < monitors.size(); i++) {
                            monitorLogDomian.monitorList.add(monitors.get(i));
                        }
                    }
                    monitorLogDomian.allList = new List[KcodeAlertAnalysisImpl.endTime];
                    for (int i = 0; i < KcodeAlertAnalysisImpl.endTime; i++) {
                        monitorLogDomian.allList[i] =  Collections.synchronizedList(new ArrayList<>());
                    }
                    if(monitorLogDomian.monitorList != null){
                        monitorLogDomian.p99String = new List[KcodeAlertAnalysisImpl.endTime];
                        monitorLogDomian.spString = new List[KcodeAlertAnalysisImpl.endTime];
                    }
                    monitorLogDomian.atomicInteger.compareAndSet(1,2);  // 其他线程修改完毕
                }
                else {
                    while (monitorLogDomian.atomicInteger.get() != 2){
                        Thread.yield();
                    }
                }
                preCallName = callName;
                preCalledName = calledName;
            }
            if(monitorLogDomian.monitorList.size() != 0){
                ips = callIp << 32 | calledIp;
                logDomains = monitorLogDomian.logMap
                        .computeIfAbsent(ips, k ->{
                            LogDomain[] logDomainArr = new LogDomain[KcodeAlertAnalysisImpl.endTime];
                                    for (int i = 0; i < KcodeAlertAnalysisImpl.endTime; i++)
                                        {logDomainArr[i] = new LogDomain();}
                            return logDomainArr;
                        });
                logDomain = logDomains[callTime];
                logDomain.addAllCount();
                if(isTrue)
                    logDomain.addTrueCount();
                logDomain.addTimeList(time);
            }
            monitorLogDomian.allList[callTime].add(time);
            if(isTrue)
                monitorLogDomian.trueC[callTime].incrementAndGet();
            monitorLogDomian.allC[callTime].incrementAndGet();
        }
    }


    private String readString(){                                // 读字符串
        int i = 0;
        while ((b = mappedByteBuffer.get(offset++)) != COMMA){
            chars[i++] = b;
        }
        return new String(chars, 0, i);
    }

    private int readInt(){                                      // 读整数
        int re = 0;
        while ((b = mappedByteBuffer.get(offset++)) != COMMA){
            re = re * 10 + b - ZERO;
        }
        return re;
    }

    private long readIp(){                                      // 读取ip
        long re = 0;
        int now;
        for (int i = 0; i < 3; i++) {
            now = 0;
            while ((b = mappedByteBuffer.get(offset++)) != POINT){
                now = now * 10 + b - ZERO;
            }
            re = (re << 8) | now;
        }
        now = 0;
        while ((b = mappedByteBuffer.get(offset++)) != COMMA){
            now = now * 10 + b - ZERO;
        }
        re = (re << 8) | now;
        return re;
    }

    private int readTime(){                                     // 读取时间
        int re = 0;
        for (int i = 0; i < 9; i++) {
            re = re * 10 + mappedByteBuffer.get(offset++) - ZERO;
        }
        offset += 5;
        return re/6 - KcodeAlertAnalysisImpl.startTime;
    }

    private boolean isTrue(){                                   // 判断是否正确
        if(mappedByteBuffer.get(offset) == 't'){
            offset+=5;
            return true;
        }
        offset+=6;
        return false;
    }

    private MappedByteBuffer mappedByteBuffer;                  // mmap
    private int eachSize;                                       // 每个线程字节数
    private int offset;                                         // 目前解析字节数
    private byte b;                                             // 当前读到字节数

    private String callName;                                    // 主调服务名
    private String calledName;                                  // 被调服务名
    private String preCallName = "";                            // 上个主调服务名
    private String preCalledName = "";                          // 上个被调服务名
    private List<MyMonitor> monitors;
    private byte[] chars = new byte[64];                        //  生成String所用
}
