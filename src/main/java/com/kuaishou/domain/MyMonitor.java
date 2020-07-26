package com.kuaishou.domain;


public class MyMonitor {
    public Integer index;      // 规则编号
    public Boolean category;   // true 为 P99，false为 SuccessPercent
    public Double threshold;  // 阈值(true为耗时，false为准确率)
    public Integer minute;        // 储存连续时间的数值

    public MyMonitor(Integer index, Boolean category, Integer minute, String threshold) {
        this.index = index;
        this.category = category;
        if(category){
            this.threshold = Double.valueOf(threshold.substring(0,threshold.length()-2));
        }else {
            this.threshold = Double.valueOf(threshold.substring(0,threshold.length()-1)) / 100;
        }
        this.minute = minute;
    }
}
