package com.kuaishou.utils;

/*
 * @Description
 * 最小堆，堆的大小为Math.floor(n * 0.01) + 1（n为该粒度日志个数）。
 * 堆不满直接加入，满了判断堆顶元素是否小于要加入的元素，是则替换，不是则不做操作。
 * @author Gwjjj
 * @date 10:34 2020/7/26
 */
public class MinHeap {
    int size;
    public int heaps[];
    int length;
    public MinHeap(int length){
        heaps = new int[length];
        this.length = length;
        size = 0;
    }

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

    private void sink(int k){
        while (k<=size/2){
            int com = 2*k;
            if(more(2*k,2*k+1))
                com ++;
            if(more(k,com)){
                swap(k,com);
                k = com;
            }
            else
                return;
        }
    }
    private void swim(int k){
        while (k != 0 && more(k/2,k)){
            swap(k,k/2);
            k = k/2;
        }
    }

    private void swap(int a, int b){
        int swap = heaps[a];
        heaps[a] = heaps[b];
        heaps[b] = swap;
    }

    private boolean more(int a, int b){
        //堆的个数为奇数时当心2*n+1溢出
        if(b<size&&heaps[a] > heaps[b])
            return true;
        return false;
    }
}

