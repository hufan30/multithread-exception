package com.github.hcsp;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MultiThreadServiceDataProcessor {
    // 线程数量
    private final int threadNumber;
    // 处理数据的远程服务
    private final RemoteService remoteService;

    public MultiThreadServiceDataProcessor(int threadNumber, RemoteService remoteService) {
        this.threadNumber = threadNumber;
        this.remoteService = remoteService;
    }

    // 将所有数据发送至远程服务处理。若所有数据都处理成功（没有任何异常抛出），返回true；
    // 否则只要有任何异常产生，返回false
    public boolean processAllData(List<Object> allData) {
        int groupSize =
                allData.size() % threadNumber == 0
                        ? allData.size() / threadNumber
                        : allData.size() / threadNumber + 1;
        List<List<Object>> dataGroups = Lists.partition(allData, groupSize);

        try {
            List<Thread> threads = new ArrayList<>();
            AtomicBoolean flag = new AtomicBoolean(true);
            for (List<Object> dataGroup : dataGroups) {
                Thread thread = new Thread(() -> {
                    try {
                        dataGroup.forEach(remoteService::processData);
                    } catch (Exception e) {
                        flag.set(false);
//                        throw new RuntimeException(e);   //虽然是throw到自己线程中，但是能知道是哪个线程，thread-10出现异常，强烈推荐此方式，养成习惯；
                        e.printStackTrace();   //单纯打印异常是没法知道是哪个线程抛出的
                    }
                });
                thread.start();
                threads.add(thread);
            }

            for (Thread thread : threads) {
                thread.join();
            }
            return flag.get();
        } catch (Exception e) {
            return false;
//            throw new RuntimeException(e);
        }
    }
}
