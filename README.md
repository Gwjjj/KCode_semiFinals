## K-Code快手设计大赛（java）

### 概要

这次比赛分为热身赛，初赛，复赛和决赛。我们组有幸以前20(排名靠后)的身份进入决赛，下面是复赛的赛题和代码。

https://github.com/Gwjjj/KCode_semiFinals

附上小数据集合

https://static.yximgs.com/kos/nlav10305/KcodeAlertAnalysis-test/KcodeAlertAnalysis-data-test.zip

---

### 思路

结果都正确的情况下，最后成绩为 N1/T1（Q1时间）*系数 + N2/T2（Q2时间），其中Q1的分比例比较低，所以把大部分计算放在Q1中。

---

### Q1

首先需要把日志文件读到内存中，初赛的io速度貌似很快，复赛被限制了，可能官方不想让比赛变成io大赛。

在初赛用的BufferedReader缓冲字符流，因为日志中除了字符串还有大部分数字和ip地址，需要二次转换，所以速度很慢。看了很多选手用的ByteBuffer块读，在复赛决定使用多线程mmap，然后读取字节自己转换（本地非常快，但复赛线上不明显）。

#### 流程

```java
/*
 * @Author             Gwjjj
 * @Description        Q1
 * @param path         日志文件路径
 * @param alertRules   监控文件列表
 * @Date               15:47 2020/7/25
 * @Return             Q1返回报警集合
 **/
 @Override
 public Collection<String> alarmMonitor(String path, Collection<String> alertRules) {
	 ...                                                         //  一些变量初始化
     init(path);                                                 //  初始化(日志开始时间，结束时间)
     dealMonitor(alertRules);                                    //  处理监控列表
     dealFile(path);                                             //  处理日志文件
     callMonitorMap = null;
     calledMonitorMap = null;
     dealMap();                                                  //  计算Q1和Q2结果
     return alarmList;                                           
 }
```

#### 初始化

初始化主要是计算出开始时间和结束时间(比赛日志的时间规则是上下浮动一分钟,+-1即可)，还要算出时间工具类的开始时间的日时分（Q2计算时间需要）

```java
/*
 * @Author Gwjjj
 * @Description                     用开始时间初始化时间工具类
 * @param time                      long 时间戳
 * @Date 22:49 2020/7/25
 * @Return
 **/
public static void TimeCastString(Long time){
    Calendar calendar=Calendar.getInstance();
    calendar.setTimeInMillis(time);
    START_DAY = calendar.get(Calendar.DATE);                        //  天
    START_HOUR_OF_DAY = calendar.get(Calendar.HOUR_OF_DAY);         //  小时
    START_MINUTE = calendar.get(Calendar.MINUTE)-1;                 //  分
}
```

#### 处理监控列表

监控中有明确主被调关系的先加入countMap(最后要计算的Map),其他出现“All”的需要在后面日志遍历过程中单独判断。

出现的数组一般都是前面初始化得出的时间区间。

```java
public static Map<String,Map<String, MonitorLogDomian>> countMap;       //  分析日志统计Map callName -> calledName
```

```java
public class MonitorLogDomian {
    public List<MyMonitor> monitorList;						// 	此服务调用对受监控的列表
    public Map<Long, LogDomain[]> logMap;					//  ip粒度对象
    public AtomicInteger atomicInteger;                     //  多线程标志位
    public AtomicInteger[] trueC;                           //  服务级别成功调用数
    public AtomicInteger[] allC;                            //  服务级别总调用数
    public List<Integer>[] allList;							//  服务级别耗时列表
    public List<String>[] p99String;                        //  q2 P99
    public List<String>[] spString;                         //  q2 SP(成功率)
    public String[] sp;                                     //  Q2 服务调用之间的sp报警信息
    public String[] p99;                                    //  Q2 服务调用之间的p99报警信息
    ...
}
```

#### 处理日志文件

```java
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
    int eachSize = 1<<18;                                               //  每次读取字节数
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
    while (true) {                                              		//等待所有任务都执行结束
        if (executorService.isTerminated()) {
            break;
        }
        Thread.yield();
    }
}
```

io线程主要就是读取字节并加入countMap，同时产生图节点和其邻接矩阵。

```java
while (offset != eachSize){
        callName = readString();
        callIp = readIp();
        calledName = readString();
        calledIp = readIp();
        isTrue = isTrue();
        time =  readInt ();
        callTime = readTime();
    	//  判断每条日志的服务对是否和前面一致，可以节省很多get()计算.
        if(!callName.equals(preCallName) || !calledName.equals(preCalledName)){
            monitorLogDomian = KcodeAlertAnalysisImpl.countMap.
                    computeIfAbsent(callName,k->new ConcurrentHashMap<>()).
                    computeIfAbsent(calledName,k->new MonitorLogDomian());
            // 相同的服务对只能由一个线程初始化
            if(monitorLogDomian.atomicInteger.compareAndSet(0,1)){  
                //  找到图节点
                callPoint = KcodeAlertAnalysisImpl.graphMap.computeIfAbsent(callName,k -> new MyPoint(callName));
                calledPoint = KcodeAlertAnalysisImpl.graphMap.computeIfAbsent(calledName,k -> new MyPoint(calledName));
                //  邻接矩阵
                callPoint.to.add(calledPoint);
                calledPoint.from.add(callPoint);
				...													//  监控信息等初始化
                monitorLogDomian.atomicInteger.compareAndSet(1,2);  // 初始化完毕
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
            ...   													//  ip粒度初始化和一些计算
    }
}
```

```java
...
private String readString(){                                // 读字符串(最耗时)
    int i = 0;
    while ((b = mappedByteBuffer.get(offset++)) != COMMA){
        chars[i++] = b;
    }
    return new String(chars, 0, i);
}

private long readIp(){                                      // 读取ip
    long re = 0L;
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
...
```

因为调用服务对会在不同分钟出现，为了防止在不同线程多余的初始化操作，在MonitorLogDomian加入atomicInteger标志位，0代表未初始化，1代表正在初始化，2代表初始化成功，使用CAS来保证线程安全，有线程安全问题的Map使用ConcurrentHashMap，List使用Collections.synchronizedList，计数则用AtomicInteger。

#### 处理countMap

此处采用多线程（就以主调名为分割加入线程池），线下时间提升还可以，线上不明显(...)

线程开始计算该服务对的ip粒度p99和sp（成功率），然后计算是否报警，生成报警字符串,同时q1报警需处理该服务对的q2。

##### 计算P99

使用一个最小堆，堆的大小为Math.floor(n * 0.01) + 1（n为该粒度日志个数）,堆不满直接加入，满了判断堆顶元素是否小于要加入的元素，是则替换，不是则不做操作。(比起排序nlog(n)的复杂度，此处是nlog(0.01n))

```java
public void addNode(int k){
    if(size == length){
        if(k > heaps[0]){
            heaps[0] = k;
            sink(0);
        }
        return;
    }
    heaps[size++] = k;
    swim(size-1);
}
```

##### 成功率

成功率要转换成字符串，保留两位小数,不需四舍五入。

> 静态工具类的对象在多线程下会出现问题（比如下面的NumberFormat,还有时间的SimpleDateFormat）。

```java
static NumberFormat nf;
static {
    nf = NumberFormat.getPercentInstance();
    //  保留两位小数
    nf.setMinimumFractionDigits(2);
    nf.setMaximumFractionDigits(2);
    //  不需要四舍五入使用RoundingMode.DOWN
    nf.setRoundingMode(RoundingMode.DOWN);
}
```

##### q1报警信息输出

使用一个滑动窗口（一个ArrayDeque，就存区间内的是否警告标志），如果当前区间都为警告则报警。

```java
Deque<Integer> deque = new ArrayDeque();
for (; i < minute; i++) {
	if(logDomains[i].p99 > monitor.threshold){
 		count++;
 		deque.addLast(1);
 	}
 	else 
 		deque.addLast(0);
    if(count == minute){
   			.... 					//  报警
 }

```

##### q2计算

主要就是找到该服务对的最长调用链，调用链用可以到达主调节点的最长路径from与从被调可以到达to的最长路径拼接而成。在dfs到一个未遍历过的节点时会把的from或是to记录下来。

```java
public static List<List<MyPoint>> findLongest(String callName, String calledName) {
    ...
    List<List<MyPoint>> fromList = findFrom(from);
    List<List<MyPoint>> toList = findTo(to);
	...
}

private static List<List<MyPoint>> findFrom(MyPoint point) {
    synchronized (point.from) {					//  只要有一个线程计算该节点的最长from和to就可以了
        if (point.longFrom != null) {
            return point.longFrom;
        } else {
            int maxLength = 0;
            List<List<MyPoint>> nowPoint;
            List<List<MyPoint>> reList = null;
            int len;
            for (int i = 0; i < point.from.size(); i++) {
                nowPoint = findFrom(point.from.get(i));
                if ((len = nowPoint.get(0).size()) > maxLength) {
            		...							//  长度超过创建新列表
                } else if (len == maxLength) {
                  	... 						//  长度相同加入列表
                }
            }
		...
        }
    }
}
```

到此为止，Q1所有和Q2的大部分计算都完成了。

---

### Q2

Q2主要都是O(1)级别的计算

```java
/*
 * @Author Gwjjj
 * @Description             快速转换时间
 * @param time              yyyy-MM-dd HH:mm
 * @Date 17:11 2020/7/26
 * @Return
 **/
public static int stringCastTime(String time){
    int h = (time.charAt(11)-'0')*10 + (time.charAt(12)-'0');
    int min = (time.charAt(14)-'0')*10+(time.charAt(15)-'0');
    min =   (h - START_HOUR_OF_DAY)*60+ (min-START_MINUTE);
    return min;
}
```

初始化计算的时分为了这边的快速计算，SimpleDateFormat.parse()很慢。（因为比赛只出现同一小时的，严谨点应该把年月日也带上）

```java
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
```

