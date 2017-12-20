package com.compucafe.executordemo;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

@Component
public class ExecutorContainer implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutorContainer.class);

    @Autowired
    ThreadPoolTaskExecutor executor;

    @Value("${com.cfa.executordemo.numTestThreads:50}")
    Integer numThreads;

    @PostConstruct
    public void initialize() {
        LOG.info("postConstruct called.");
        Thread execThread = new Thread(this);
        execThread.setName("execThread");
        execThread.start();
    }

    public void run() {
        List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();
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
            ListenableFuture<Boolean> lf = executor.submitListenable(rt);
            lf.addCallback(new ListenableFutureCallback<Boolean>() {
                @Override
                public void onFailure(Throwable throwable) {
                    LOG.info("Failure of thread", throwable);
                }

                @Override
                public void onSuccess(Boolean aBoolean) {
                    LOG.info("Successful callback.");
                }
            });
            futures.add(lf);
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

    static class RunnerTask implements Callable<Boolean> {
        @Getter
        String name;

        RunnerTask(String name) {
            this.name = name;
        }

        public Boolean call() throws Exception {
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
            boolean retVal = true;
            Throwable t = null;
            int actionCode = (int)(Math.random() * 4);
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
                    throw new Exception("throw checked exception");
                case 0:
                    retVal = false;
                    throw new RuntimeException("throw unchecked exception.");
                default:
                    retVal = false;
                    throw new Exception("Unknown case for switch statement. value: " + actionCode);
            }


            LOG.info(String.format("[%s] 0x[%s] thread slept for [%d] milliseconds, returning [%s]", name,
                    Integer.toHexString(this.hashCode()), waitTime, retVal));
            return retVal;
        }
    }
}
