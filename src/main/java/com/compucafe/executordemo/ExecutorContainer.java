package com.compucafe.executordemo;

import lombok.Getter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

@Component
public class ExecutorContainer implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutorContainer.class);

    @Autowired
    ThreadPoolTaskExecutor executor;

    @Value("${com.compucafe.executor-demo.numTestThreads:50}")
    Integer numThreads;

    @Value("${com.compucafe.executor-demo.submitRetryWaitMs:5000}")
    long submitRetryWaitMs;

    @Value("${com.compucafe.executor-demo.submitWaitMs:250}")
    long submitWaitMs;

    @Value("${com.compucafe.executor-demo.taskPollWaitMs:1000}")
    long taskPollWaitMs;

    boolean running = false;


    private void initialize() {
        LOG.info("initialize called.");
        Thread execThread = new Thread(this);
        execThread.setName("execThread");
        execThread.start();
        synchronized (this) {
            running = true;
        }
    }

    @EventListener
    public void handleRefresh(ContextRefreshedEvent event) {
        LOG.info("Context event received: " + ToStringBuilder.reflectionToString(event));
        if (!running) {
            initialize();
        }
        LOG.debug("DEBUG enabled.");
    }


    public void run() {
        List<Future<RunnerTask>> futures = new ArrayList<Future<RunnerTask>>();
        LOG.info("exec thread started.");
        for (int i = 1; i < numThreads + 1; i++) {
            try {
                synchronized (this) {
                    wait(submitWaitMs);
                }
            } catch (InterruptedException ie) {
                LOG.error("Interrupted thread!");
            }
            RunnerTask rt = new RunnerTask(String.format("task-%03d", i));
            LOG.info("Starting task: " + rt.getName() + " 0x" + Integer.toHexString(rt.hashCode()));
            ListenableFuture<RunnerTask> lf = null;
            try {
                lf = executor.submitListenable(rt);
            } catch (TaskRejectedException tre) {
                LOG.warn("Failed to submit task " + rt.getName() + " waiting for 5 seconds before trying again...");
                try {
                    synchronized (this) {
                        wait(submitRetryWaitMs);
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
                    LOG.info("[" + success.getName() + "] success... " +  ToStringBuilder.reflectionToString(success));
                    synchronized (this) {
                        notify();
                    }
                }, failure -> {
                    LOG.info("failure..." + ToStringBuilder.reflectionToString(failure) +
                            " this: " + ToStringBuilder.reflectionToString(this));
                    if (failure instanceof ExecutorException) {
                        LOG.info("[" + ((ExecutorException) failure).getFailedTask().getName() + "] " + ((ExecutorException) failure).getFailedTask().toString(), failure.getCause());
                    }
                    synchronized (this) {
                        notify();
                    }
                });
                futures.add(lf);
            }
        }
        LOG.info("submitted all executables...");
        int active = executor.getActiveCount();
        while (active > 0) {
            synchronized (this) {
                try {
                    long timeWaitStarted = System.currentTimeMillis();
                    wait(taskPollWaitMs);
                    long timeRemianing = System.currentTimeMillis() - timeWaitStarted;
                    LOG.debug(String.format("thread notified. time remaining (millis) [%d]", timeRemianing));
                } catch (InterruptedException ie) {
                    LOG.error("thread interrupted...", ie);
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
                    LOG.info("Thread interrupted...", ie);
                }
            }

            Throwable t          = null;
            int       actionCode = (int) (Math.random() * 4);
            LOG.info("actionCode: " + actionCode);
            switch (actionCode) {
                case 3:
                    result = true;
                    break;
                case 2:
                    result = false;
                    break;
                case 1:
                    result = false;
                    Exception ex = new Exception("Thrown from Runnertask");
                    ex.fillInStackTrace();
                    throw new ExecutorException("throw checked exception", ex, this);
                case 0:
                    result = false;
                    RuntimeException rex = new RuntimeException("Thrown in RunnerTask");
                    rex.fillInStackTrace();
                    throw new ExecutorException("throw unchecked exception.", rex, this);
                default:
                    result = false;
                    throw new ExecutorException("Unknown case for switch statement. value: " + actionCode, null, this);
            }


            LOG.info(String.format("[%s] 0x[%s] thread slept for [%d] milliseconds, returning [%s]", name,
                    Integer.toHexString(this.hashCode()), waitTime, result));


            return this;
        }
        public String toString() {
            return new ToStringBuilder(this).append(this.name).append(this.result).append(this.thrown).toString();
        }
    }
    public static class ExecutorException extends Exception {
        @Getter
        RunnerTask failedTask;
        ExecutorException(String msg, Throwable cause, RunnerTask task) {
            super(msg, cause);
            failedTask = task;
        }
    }

}
