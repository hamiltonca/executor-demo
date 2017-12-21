package com.compucafe.executordemo;

import lombok.Getter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

@Component
public class ExecutorContainer implements Runnable, InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutorContainer.class);

    @Autowired
    ThreadPoolTaskExecutor executor;

    @Value("${com.compucafe.executor-demo.numTestThreads:50}")
    Integer numThreads;

    public void afterPropertiesSet() throws Exception {
        LOG.info("afterPropertiesSet called...");
        initialize();
    }

    //    @PostConstruct
    public void initialize() {
        LOG.info("initialize called.");
        Thread execThread = new Thread(this);
        execThread.setName("execThread");
        execThread.start();
    }

    public void run() {
        List<Future<RunnerTask>> futures = new ArrayList<Future<RunnerTask>>();
        LOG.info("exec thread started.");
        for (int i = 1; i < numThreads + 1; i++) {
            try {
                synchronized (this) {
                    wait(250L);
                }
            } catch (InterruptedException ie) {
                LOG.error("Interrupted thread!");
            }
            RunnerTask rt = new RunnerTask("task-" + i);
            LOG.info("Starting task: " + rt.getName() + " 0x" + Integer.toHexString(rt.hashCode()));
            ListenableFuture<RunnerTask> lf = null;
            try {
                lf = executor.submitListenable(rt);
            } catch (TaskRejectedException tre) {
                LOG.warn("Failed to submit task " + rt.getName() + " waiting for 5 seconds before trying again...");
                try {
                    synchronized (this) {
                        wait(5000);
                    }
                } catch (InterruptedException ie) {
                    LOG.warn("Exception waiting to restart submitting tasks...");
                }
                try {
                    LOG.info("Starting task: " + rt.getName() + " 0x" + Integer.toHexString(rt.hashCode()) + " after it failed to be submitted the first time.");
                    lf = executor.submitListenable(rt);
                } catch (TaskRejectedException tre1) {
                    LOG.error("Failed on retry to submit task " + rt.getName() + " exception ignored... continuing.", tre1);
                }

            }
            if (lf != null) {
                lf.addCallback(success -> {
                    LOG.info("success... " + ToStringBuilder.reflectionToString(success));
                }, failure -> {
                    LOG.info("failure..." + ToStringBuilder.reflectionToString(failure));
                });
                futures.add(lf);
            }
        }
        LOG.info("submitted all executables...");
        int active = executor.getActiveCount();
        while (active > 0) {
            synchronized (this) {
                try {
                    wait(1000L);
                } catch (InterruptedException ie) {
                    LOG.error("thread interrupted...");
                }
            }
            active = executor.getActiveCount();
            LOG.info(String.format("waiting on active threads: [%d]", active));
        }
        LOG.info("task starter thread finished...");
        executor.destroy();

    }

    static class RunnerTask implements Callable<RunnerTask> {
        @Getter
        String name;

        @Getter
        Boolean   result;
        @Getter
        Throwable thrown;

        RunnerTask(String name) {
            this.name = name;
        }

        public RunnerTask call() throws Exception {
            long waitTime = (long) (Math.random() * 30000);
            LOG.info(String.format("[%s] 0x[%s] Sleeping for [%d] milliseconds...", name,
                    Integer.toHexString(this.hashCode()), waitTime));
            synchronized (this) {
                try {
                    wait(waitTime);
                } catch (InterruptedException ie) {
                    LOG.info("Thread interrupted...");
                }
            }
            boolean   retVal     = true;
            Throwable t          = null;
            int       actionCode = (int) (Math.random() * 4);
            LOG.info("actionCode: " + actionCode);
            switch (actionCode) {
                case 3:
                    retVal = true;
                    break;
                case 2:
                    retVal = false;
                    break;
                case 1:
                    retVal = false;
                    thrown = new Exception("throw checked exception");
                    throw (Exception) thrown;
                case 0:
                    retVal = false;
                    thrown = new RuntimeException("throw unchecked exception.");
                    throw (RuntimeException) thrown;
                default:
                    retVal = false;
                    thrown = new Exception("Unknown case for switch statement. value: " + actionCode);
                    throw (RuntimeException) thrown;
            }


            LOG.info(String.format("[%s] 0x[%s] thread slept for [%d] milliseconds, returning [%s]", name,
                    Integer.toHexString(this.hashCode()), waitTime, retVal));
            result = retVal;

            return this;
        }
    }
}
