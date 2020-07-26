package com.kuaishou.domain;

import java.util.List;

import java.util.ArrayList;
import java.util.Collections;

//  稀疏图   邻接矩阵
public class MyPoint {
    public String name;                             //  服务名
    public List<MyPoint> from;                      //  列表中的节点都可以到达此节点
    public List<MyPoint> to;                        //  列表中的节点都可以由此节点到达
    public List<List<MyPoint>> longFrom;            //  记录到达此节点的最长调用链(遇到才会初始化)
    public List<List<MyPoint>> longTo;              //  记录由此节点出发的最长调用链(遇到才会初始化)

    public MyPoint(String name) {
        this.name = name;
        this.from = Collections.synchronizedList(new ArrayList<>());
        this.to = Collections.synchronizedList(new ArrayList<>());
    }
}