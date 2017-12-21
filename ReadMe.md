# Executor-demo

This is a simple Spring boot application that runs a collection of threads 
on a ThreadPoolTaskExecutor. The executor can be fully configured with the 
application.properties file.

The application builds to a Spring Boot executable jar using Maven.

To build the app

```mvn package```

To run the app

```java -jar target/executor-demo-0.0.1-SNAPSHOT.jar```

This is really a simple app wit few dependencies (beyond the world of spring boot).

This app gives you the ability to configure/reconfigure a [Spring](https://spring.io) [ThreadPoolTaskExecutor]( https://docs.spring.io/spring-framework/docs/4.3.13.RELEASE/javadoc-api/org/springframework/scheduling/concurrent/ThreadPoolTaskExecutor.html )

The ThreadPoolTaskExecutor wraps the Java [ThreadPoolExecutor](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ThreadPoolExecutor.html) and configures it using bean injection.

The javadoc on the ThreadPoolExecutor has some very good information for those wishing to use this class. 
It is also recommended reading wishing to understand thread use and management.

The class also initializes the ThreadPoolExecutor including setting up a BlockingQueue for it. 
If the queue size is zero then a [```SynchronousQueue<Runnable>```](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/SynchronousQueue.html)
is initialized. 
This means only one task is ever at risk of loss due to a JVM crash. 
If the value is greater than 0 then a [```LinkedBlockingQueue<Runnable>```](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/LinkedBlockingQueue.html) is initialized and used.  

Enjoy.