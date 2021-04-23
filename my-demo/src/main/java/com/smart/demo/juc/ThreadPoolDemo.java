package com.smart.demo.juc;

import lombok.extern.slf4j.Slf4j;

import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author lukewei
 * @date 2021/4/23 14:32
 *
 * 线程池
 */
@Slf4j
public class ThreadPoolDemo {


    static class ThreadPoolProvider{

        public ThreadPoolProvider() {
        }


        public static ExecutorService newFixedThreadPool() {
            return FIXED_THREAD_POOL;
        }

        /**
         * 自定义线程池
         * param:
         *      cupCoreSize():核心线程数
         *      cupCoreSize()*2:最大线程数
         *      10:线程回收时间
         *      TimeUnit.SECONDS: 线程回收时间单位，秒
         *      阻塞队列
         *      自定义工厂
         *      自定义拒绝策略
         *
         * 执行流程：
         *  1.线程池初始化时会创建8个核心线程池；
         *  2.任务进来时，先将任务放到阻塞队列里由核心线程进行处理；
         *  3.当阻塞队列满列时，线程池会将核心线程数扩大到最大线程数，为16个去处理任务；
         *  4.当队列满时再进入的任务执行拒绝策略(丢弃？中断当前线程？将该任务回收等待空闲线程来处理？)
         *  5.当阻塞队列中任务全部执行完成后，将在10S内回收线程，将线程池中的线程由最大线程数回收成核心线程数(16->8)
         */
        private static final ExecutorService FIXED_THREAD_POOL = new ThreadPoolExecutor(cupCoreSize(),
                cupCoreSize()*2,
                10,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(100),
                new DefaultThreadFactory(),
                new CustomRejectedExecutionHandler());


        /**
         * 自适应获取当前CPU核数大小，默认8
         * @return 返回CPU核数大小
         */
        private static Integer cupCoreSize(){
            return Runtime.getRuntime().availableProcessors() != 0 ? Runtime.getRuntime().availableProcessors():8;
        }

        /**
         * 自定义工程模式
         */
        private static class DefaultThreadFactory implements ThreadFactory{
            private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);
            private final ThreadGroup group;
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            private final String namePrefix;

            /**
             * 工厂名称前缀
             */
            DefaultThreadFactory() {
                this("myThread-pool-t-");
            }


            DefaultThreadFactory(String prefix) {
                SecurityManager s = System.getSecurityManager();
                group = (s != null) ? s.getThreadGroup() :
                        Thread.currentThread().getThreadGroup();
                namePrefix = prefix + POOL_NUMBER.getAndIncrement();
            }

            @Override
            public Thread newThread(Runnable runnable) {
                Thread t = new Thread(group, runnable,
                        namePrefix + threadNumber.getAndIncrement(),
                        0);
                if (t.isDaemon()) {
                    t.setDaemon(false);
                }
                if (t.getPriority() != Thread.NORM_PRIORITY) {
                    t.setPriority(Thread.NORM_PRIORITY);
                }
                return t;
            }
        }

        /**
         * 自定义线程池策略
         * 线程被拒绝后，重新开启一个线程执行被拒绝线程
         */
        private static class CustomRejectedExecutionHandler implements RejectedExecutionHandler{

            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                log.info("New Thread begin run which thread is Rejected");
                ThreadPoolProvider.newFixedThreadPool().execute(r);
            }
        }

    }


    /**
     * 测试main方法
     * 1.如果将 i设定为小于100的值，会发现线城池中的线程会是1-8
     * 2.如果将 i设定为大于100的值，会发现线线城中的线程会有16个
     * @param args args
     */
    public static void main(String[] args) {
        for (int i = 0; i < 100; i++) {
            ThreadPoolProvider.FIXED_THREAD_POOL.execute(()->{
                log.info(new Random().nextInt()+"");
            });
        }

    }

}
