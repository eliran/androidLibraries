package com.threeplay.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by eliranbe on 2/12/17.
 */

public class JobQueue<T> {

    public static class Policy {
        private int maximumNumberOfConcurrentJobs;

        public Policy(int maximumNumberOfConcurrentJobs){
            this.maximumNumberOfConcurrentJobs = maximumNumberOfConcurrentJobs;
        }
    }

    public interface Job<T> {
        void onQueued();
        void onCanceled();
        T onExecute() throws Exception;
    }

    public static class Manager {
        private static final Manager instance = new Manager();
        private Map<String, JobQueue> queues = new HashMap<>();


        public void registerQueue(String name, Policy policy){

        }

        public <T> JobQueue<T> getQueue(String name){
            return null;
        }

    }

    private class QueuedJob<T> {
        private final Job<T> job;
        private final Promise.Defer<T> defer = Promise.defer();

        QueuedJob(Job<T> job){
            this.job = job;
            job.onQueued();
        }

        Promise<T> getPromise(){
            return defer.promise;
        }

        boolean cancel(){
            job.onCanceled();
            return true;
        }

        boolean execute(){
            try {
                defer.resolveWithResult(job.onExecute());
            } catch ( Exception e ) {
                defer.rejectWithException(e);
            }
            return true;
        }
    }

    private LinkedList<QueuedJob<T>> jobs = new LinkedList<>();

    public JobQueue(){
    }

    public synchronized Promise<T> submit(Job<T> job){
        QueuedJob<T> queuedJob = new QueuedJob<>(job);
        jobs.add(queuedJob);
        return queuedJob.getPromise();
    }

    public synchronized boolean cancel(Job<T> job) {
        if ( job != null ) {
            Iterator<QueuedJob<T>> iterator = jobs.iterator();
            while (iterator.hasNext()) {
                QueuedJob<T> queuedJob = iterator.next();
                if (queuedJob.job == job) {
                    if ( queuedJob.cancel() ) {
                        iterator.remove();
                        return true;
                    }
                    return false;
                }
            }
        }
        return false;
    }

    public boolean next(){
        QueuedJob<T> job = null;
        synchronized(this) {
            if ( jobs.size() > 0 ) {
                job = jobs.remove();
            }
        }
        return job != null && job.execute();
    }

}
