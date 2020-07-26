package com.kuaishou.kcode;

import com.kuaishou.domain.*;
import com.kuaishou.utils.*;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author KCODE
 * Created on 2020-07-04
 */
public class KcodeAlertAnalysisImpl implements KcodeAlertAnalysis {
    private static String ALL = "ALL";

   /*
    * @Author              Gwjjj
    * @Description         Q1
    * @param path         日志文件路径
    * @param alertRules   监控文件路径
    * @Date               15:47 2020/7/25
    * @Return             Q1返回报警集合
    **/
    @Override
    public Collection<String> alarmMonitor(String path, Collection<String> alertRules) {
        graphMap = new ConcurrentHashMap<>();                       //  服务名对应的邻接矩阵
        callMonitorMap = new HashMap<>();                           //  监控主调-All
        calledMonitorMap = new HashMap<>();                         //  监控中All-被调
        countMap = new ConcurrentHashMap<>(1<<5);       //  所有主被调集合
        alarmList = Collections.synchronizedList
                (new ArrayList<>(1<<11));               //  Q1返回报警集合
        init(path);                                                 //  初始化(日志开始时间，结束时间)
        dealMonitor(alertRules);                                    //  处理监控文件
        dealFile(path);                                             //  处理日志文件
        callMonitorMap = null;
        calledMonitorMap = null;
        dealMap();                                                  //  计算Q1和Q2结果
        return alarmList;
    }

    /*
     * @Author Gwjjj
     * @Description                     Q2
     * @param caller                    主调服务名
     * @param responder                 被调服务名
     * @param time                      监控时间 yyyy-MM-dd HH:mm
     * @param type                      监控类型 P99 SP
     * @Date 15:49 2020/7/25
     * @Return
     **/
    @Override
    public Collection<String> getLongestPath(String caller, String responder, String time, String type){
        if(type.length() == 3){
            return countMap.get(caller).get(responder).p99String[TimeInit.stringCastTime(time)];
        }
        return countMap.get(caller).get(responder).spString[TimeInit.stringCastTime(time)];
    }



    // 处理报警规则
    private void dealMonitor(Collection<String> alertRules){
        String index;
        String callName;
        String calledName;
        String category;
        String minute;
        String threshold;
        StringTokenizer st;
        List<MyMonitor> myMonitors;
        for (String monitorStr : alertRules) {
            st = new StringTokenizer(monitorStr,",");
            index = st.nextToken();
            callName = st.nextToken();
            calledName = st.nextToken();
            category = st.nextToken();
            minute = st.nextToken();
            threshold = st.nextToken();
            if(isAll(callName)){
                myMonitors = calledMonitorMap.computeIfAbsent(calledName,k ->{
                    ArrayList arrayList = new ArrayList();
                    return arrayList;
                });
            }
            else if(isAll(calledName)){
                myMonitors = callMonitorMap.computeIfAbsent(callName,k ->{
                    ArrayList arrayList = new ArrayList();
                    return arrayList;
                });
            }
            else {
                myMonitors = countMap.computeIfAbsent(callName,k ->new ConcurrentHashMap<String,MonitorLogDomian>()).
                        computeIfAbsent(calledName, k->{
                            MonitorLogDomian monitorLogDomian = new MonitorLogDomian();
                            return monitorLogDomian;
                        }).monitorList;
            }
            myMonitors.add(
                    new MyMonitor(Integer.valueOf(index),KcodeAlertAnalysis.ALERT_TYPE_P99.equals(category),
                            Integer.valueOf(minute.substring(0,minute.length()-1)),
                            threshold));
        }

    }

    /*
     * @Author Gwjjj
     * @Description                     处理日志文件,每次映射固定大小mmap,找到最后的空行分割，加入线程池
     * @param filePath                  日志文件路径
     * @Date 16:11 2020/7/25
     * @Return
     **/
    private void dealFile(String filePath){
        byte b;                                                             //  当前读取一字节
        long cur = 0L;                                                      //  每次开始的偏移量
        int eachSize = 1<<18;                                               //  每次读取量级
        try(FileChannel fChannel = new FileInputStream(new File(filePath)).getChannel()){
            long length = fChannel.size();                                  //  文件长度（字节）
            while (cur < length) {
                MappedByteBuffer mappedByteBuffer = fChannel.map(FileChannel.MapMode.READ_ONLY, cur, eachSize);
                int offset = eachSize - 1;
                for (; offset > 0; offset--) {
                    b = mappedByteBuffer.get(offset);
                    if(b == IOThread.SPACE_LINE){                           // 从最后找第一个空行分割
                        offset++;
                        break;
                    }
                }
                IOThread ioThread = new IOThread(mappedByteBuffer,offset);  // 加入io线程池
                executorService.execute(ioThread);                          // 执行
                cur += offset;                                              // 更新已读字节数
                if(length-cur < eachSize){
                    eachSize = (int)(length-cur);
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        executorService.shutdown();
        while (true) {                                              //等待所有任务都执行结束
            if (executorService.isTerminated()) {
                break;
            }
            Thread.yield();
        }
    }


    private void dealMap(){
        String callName;
        String calledName;
        for (Map.Entry<String,Map<String,MonitorLogDomian>> countEntry:countMap.entrySet()) {
            callName = countEntry.getKey();
            Map<String, MonitorLogDomian> callMap = countEntry.getValue();
            for (Map.Entry<String, MonitorLogDomian> callEntry: callMap.entrySet()) {
                calledName = callEntry.getKey();
                MonitorLogDomian mLogDomian = callEntry.getValue();
                LogDomianThread logDomianThread = new LogDomianThread(mLogDomian,callName,calledName);
                logExecutorService.execute(logDomianThread);
            }
        }
        logExecutorService.shutdown();
        while (true) {                                              //等待所有任务都执行结束
            if (logExecutorService.isTerminated()) {                //所有的子线程都结束了
                break;
            }
            Thread.yield();
        }
    }

    // 判断方法名是否为ALL
    private boolean isAll(String name){
        return ALL.equals(name);
    }


    /*
     * @Author Gwjjj
     * @Description         初始化(日志开始时间，结束时间)
     * @param filePath      日志文件路径
     * @Date 16:02 2020/7/25
     * @Return
     **/
    private void init(String filePath) {
        try (FileInputStream finStream = new FileInputStream(new File(filePath));
             InputStreamReader inReader = new InputStreamReader(finStream);
             BufferedReader buReader = new BufferedReader(inReader)) {
            String first = buReader.readLine();
            String sTime = first.split(",")[6];
            Long aLong = Long.valueOf(sTime);
            TimeInit.TimeCastString(aLong);
            startTime = (int)(aLong/60000) - 1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        try (FileChannel fChannel = new FileInputStream(new File(filePath)).getChannel()) {
            long length = fChannel.size();                                     // 文件长度（字节）
            endTime = 0;
            MappedByteBuffer mappedByteBuffer = fChannel.map(FileChannel.MapMode.READ_ONLY, length-14, 13);
            for (int i = 0; i < 10; i++) {
                endTime = endTime * 10 + mappedByteBuffer.get(i) - IOThread.ZERO;
            }
            endTime /= 60;
            endTime -= startTime - 1;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static Map<String, MyPoint> graphMap;
    public static int startTime;                                            // 开始时间
    public static int endTime;                                              // 结束时间
    public static Map<String, List<MyMonitor>> callMonitorMap;              // 主调监控,被调为ALL
    public static Map<String, List<MyMonitor>> calledMonitorMap;            // 被调监控，主调为ALL
    public static Map<String,Map<String, MonitorLogDomian>> countMap;       // 分析日志统计Map callName -> calledName
    public static List<String> alarmList;  // Q1返回
    private ExecutorService executorService =
            Executors.newFixedThreadPool(14);                        //处理线程池

    private ExecutorService logExecutorService =
            Executors.newFixedThreadPool(14);                        //处理线程池
//    private ExecutorService executorService = Executors.newSingleThreadExecutor();    // 单线程测试
}