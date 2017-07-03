package com.threeplay.core;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNot.not;

/**
 * Created by eliranbe on 2/12/17.
 */

public class JobQueueTest {

    public JobQueue<Boolean> jobQueue;


    static class MockJob implements JobQueue.Job<Boolean> {
        private Exception exception = null;
        private AtomicBoolean trigger = new AtomicBoolean(true);
        private AtomicBoolean triggered = new AtomicBoolean(false);

        public void triggerExecute(){
            trigger.set(true);
        }

        public void clearTrigger(){
            trigger.set(false);
        }

        public void waitTriggered(){
            triggerExecute();
            while ( !triggered.get() ) {
                try { Thread.sleep(1); } catch (InterruptedException ok) {}
            }
        }

        public String log = "";

        MockJob(){
            this(null);
        }

        MockJob(Exception e){
            this.exception = e;
        }

        @Override
        public void onQueued() {
            log = log + "q";
        }

        @Override
        public void onCanceled() {
            log = log + "c";
        }

        @Override
        public Boolean onExecute() throws Exception {
            log = log + "e";
            while ( !trigger.get() ) {
                try { Thread.sleep(1); } catch ( InterruptedException ok ) {}
            }
            triggered.set(true);
            if ( exception != null ) {
                throw exception;
            }
            return true;
        }
    }

    @Before
    public void setup(){
        jobQueue = new JobQueue<>();
    }

    @Test
    public void queue_returns_a_promise_from_when_submitted(){
        MockJob job = new MockJob();
        assertThat(jobQueue.submit(job), is(not(nullValue())));
        assertThat(job.log, is("q"));
    }

    @Test
    public void when_submitting_a_job_to_a_queue_the_job_onQueued_method_is_called(){
        MockJob job = new MockJob();
        assertThat(jobQueue.submit(job), is(not(nullValue())));
        assertThat(job.log, is("q"));
    }


    @Test
    public void when_a_job_is_canceled_the_onCanceled_method_is_called(){
        MockJob job = new MockJob();
        jobQueue.submit(job);
        assertThat(jobQueue.cancel(job), is(true));
        assertThat(job.log, is("qc"));
    }

    @Test
    public void returned_promise_is_triggred_with_result_from_jobs_onExecute(){
        MockJob job = new MockJob();
        Promise<Boolean> p = jobQueue.submit(job);
        jobQueue.next();
        assertThat(job.log, is("qe"));
        assertThat(p.wasTriggered(), is(true));
        assertThat(p.getResult(), is(true));
    }

    @Test
    public void returned_promise_is_triggered_with_exception_from_jobs_onExecute(){
        MockJob job = new MockJob(new Exception("test"));
        Promise<Boolean> p = jobQueue.submit(job);
        jobQueue.next();
        assertThat(job.log, is("qe"));
        assertThat(p.wasTriggered(), is(true));
        assertThat(p.getException().getMessage(), is("test"));
    }

    @Test
    public void cannot_cancel_a_job_not_in_the_queue(){
        MockJob job = new MockJob();
        assertThat(jobQueue.cancel(job), is(false));
    }

    @Test
    public void cannot_cancel_a_job_that_already_executing(){
        MockJob job = new MockJob();
        job.clearTrigger();
        final AtomicBoolean running = new AtomicBoolean(false);
        Promise<Boolean> p = jobQueue.submit(job);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                running.set(true);
                jobQueue.next();
            }
        });
        t.start();
        while ( !running.get() ) {
            try { Thread.sleep(1); } catch ( InterruptedException ok ) {}
        }
        boolean wasntTriggeredBefore = p.wasTriggered();
        boolean wasCanceled = jobQueue.cancel(job);
        job.waitTriggered();
        try { t.join(); } catch ( InterruptedException ok ) {}
        boolean wasTriggeredAfter = p.wasTriggered();

        assertThat(wasntTriggeredBefore, is(false));
        assertThat(wasCanceled, is(false));
        assertThat(wasTriggeredAfter, is(true));
    }
}
