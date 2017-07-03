package com.threeplay.core;

import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.Matchers.notNull;

/**
 * Created by eliranbe on 7/8/15.
 */
public class PromiseTest {

    private PromiseTester tester;
    private Promise.Defer<Integer> deferred;

    @Before
    public void promise_tester(){
        tester = new PromiseTester();
        deferred = Promise.defer();
    }

    @Test
    public void create_a_deferred_promise(){
        assertThat(deferred.promise.wasTriggered(), is(false));
    }

    @Test
    public void a_deferred_promise_with_successful_trigger() throws Exception {
        deferred.resolveWithResult(1);
        assertThat(deferred.promise.wasTriggered(), is(true));
        assertThat(deferred.promise.wasSuccessful(), is(true));
        assertThat(deferred.promise.getResult(), is(1));
    }

    @Test
    public void a_deferred_promise_with_failure_trigger() throws Exception {
        Exception e = new Exception();
        deferred.rejectWithException(e);
        assertThat(deferred.promise.wasTriggered(), is(true));
        assertThat(deferred.promise.wasSuccessful(), is(false));
        assertThat(deferred.promise.getException(), is(e));
    }

    @Test
    public void promise_from_handler(){
        final boolean[] triggered = new boolean[1];
        Promise.withHandler(new Promise.Handler(){
            @Override
            public void trigger(Promise.Triggered p) throws Exception {
                assertThat(p.getResult(), is(nullValue()));
                assertThat(p.getException(), is(nullValue()));
                triggered[0] = true;
            }
        });
        assertThat(triggered[0], is(true));
    }

    @Test
    public void promise_from_handler_with_initial_value(){
        final boolean[] triggered = new boolean[1];
        Promise.withHandler(123, new Promise.Handler<Integer>(){
            @Override
            public void trigger(Promise.Triggered<Integer> p) throws Exception {
                assertThat(p.getResult(), is(123));
                assertThat(p.getException(), is(nullValue()));
                triggered[0] = true;
            }
        });
        assertThat(triggered[0], is(true));
    }

    @Test
    public void promise_with_initial_result(){
        Promise p = Promise.withResult(123);
        assertThat((Integer)p.getResult(), is(123));
        p.then(tester.handler());
        tester.assertInvoked();
    }

    @Test
    public void promise_with_initial_exception(){
        Exception e = new Exception();
        Promise p = Promise.withException(e);
        assertThat(p.getException(), is(e));
        p.fail(tester.handler());
        tester.assertInvoked();
    }

    @Test
    public void resolveWithResult_returns_null_if_was_already_triggered() throws Exception {
        assertThat(deferred.resolveWithResult(0), is(deferred.promise));
        assertThat(deferred.resolveWithResult(0), is(nullValue()));
    }

    @Test
    public void rejectWithException_returns_null_if_was_already_triggered() throws Exception {
        assertThat(deferred.rejectWithException(new Exception()), is(deferred.promise));
        assertThat(deferred.rejectWithException(new Exception()), is(nullValue()));
    }

    @Test
    public void chain_promises_with_then() throws Exception {
        deferred.then(tester.handler());
        assertSuccessHandlerInvoked(deferred, tester);
    }

    @Test
    public void chain_promises_with_extended_then_and_succuess(){
        deferred.then(tester.handler("s"), tester.handler("f"));
        deferred.resolveWithResult(0);
        assertThat(tester.log, is("s"));
    }

    @Test
    public void chain_promises_with_extended_then_and_failure(){
        deferred.then(tester.handler("s"), tester.handler("f"));
        deferred.rejectWithException(new Exception());
        assertThat(tester.log, is("f"));
    }

    @Test
    public void chain_promises_with_fail() throws Exception {
        deferred.fail(tester.handler());
        assertFailureHandlerInvoked(deferred, tester);
    }

    @Test
    public void chain_promises_with_any_trigger_by_failure() throws Exception {
        deferred.any(tester.handler());
        assertFailureHandlerInvoked(deferred, tester);
    }

    @Test
    public void chain_promises_with_any_trigger_by_success() throws Exception {
        deferred.any(tester.handler());
        assertSuccessHandlerInvoked(deferred, tester);
    }

    @Test
    public void trigger_chain_of_promises() throws Exception {
        deferred.then(tester.handler("1")).then(tester.handler("2"));
        deferred.resolveWithResult(0);
        assertThat(tester.log, is("12"));
    }

    @Test
    public void switch_from_success_to_failure_by_returning_method() throws Exception {
        deferred.then(new Promise.Handler() {
            @Override
            public void trigger(Promise.Triggered p) throws Exception {
                p.failureWithException(new Exception("e"));
            }
        }).fail(tester.logException());
        deferred.resolveWithResult(0);
        assertThat(tester.log, is("e"));
    }

    @Test
    public void switch_from_failure_to_success_by_calling_successWithResult() throws Exception {
        deferred.fail(new Promise.Handler() {
            @Override
            public void trigger(Promise.Triggered p) throws Exception {
                p.successfulWithResult(0);
            }
        }).then(tester.handler("s"));
        deferred.rejectWithException(new Exception());
        assertThat(tester.log, is("s"));
    }

    @Test
    public void switch_from_success_to_failure_by_catching_exception() throws Exception {
        deferred.then(new Promise.Handler() {
            @Override
            public void trigger(Promise.Triggered p) throws Exception {
                throw new Exception("catch");
            }
        }).fail(tester.logException());
        deferred.resolveWithResult(0);
        assertThat(tester.log, is("catch"));
    }

    @Test
    public void then_chains_handlers() throws Exception {
        Promise p1 = deferred.then(tester.handler("1"));
        Promise p2 = deferred.then(tester.handler("2"));
        assertThat(p1, is(not(p2)));
        deferred.resolveWithResult(0);
        assertThat(tester.log, is("12"));
    }

    @Test
    public void fail_chains_handles() throws Exception {
        Promise p1 = deferred.fail(tester.handler("1"));
        Promise p2 = deferred.fail(tester.handler("2"));
        assertThat(p1, is(not(p2)));
        deferred.rejectWithException(new Exception());
        assertThat(tester.log, is("12"));
    }

    @Test
    public void any_chains_handles() throws Exception {
        Promise p1 = deferred.any(tester.handler("1"));
        Promise p2 = deferred.any(tester.handler("2"));
        assertThat(p1, is(not(p2)));
        deferred.rejectWithException(new Exception());
        assertThat(tester.log, is("12"));
    }

    @Test
    public void late_binding_trigger(){
        deferred.resolveWithResult(0);
        deferred.then(tester.handler("1")).then(tester.handler("2"));
        assertThat(tester.log, is("12"));
    }

    @Test
    public void only_one_thread_is_able_to_resolve_a_deferred_promise(){
        final ThreadTesting threads = new ThreadTesting();
        threads.startThread(100, new Runnable() {
            @Override
            public void run() {
                while ( threads.deferred == null ) Thread.yield();
                threads.deferred.resolveWithResult(0);
            }
        });
        deferred.then(tester.handler("1"));
        threads.deferred = deferred;
        threads.join(10);
        assertThat(tester.log, is("1"));
    }

    @Test
    public void get_untriggered_promise(){
        Promise p = Promise.promise();
        assertThat(p.getResult(), is(nullValue()));
        assertThat(p.getException(), is(nullValue()));
        assertThat(p.wasTriggered(), is(false));
    }

    @Test
    public void continueWithPromise_processes_promises_before_continuing_with_promise_chain(){
        final Promise.Defer innerDefer = Promise.defer();
        deferred.promise.then(new Promise.Handler() {
            @Override
            public void trigger(Promise.Triggered p) throws Exception {
                p.continueAfterPromise(innerDefer.promise.then(tester.handler("1")));
            }
        }).any(tester.handler("2"));
        deferred.resolveWithResult(0);
        assertThat(tester.log,is (""));
        innerDefer.resolveWithResult(0);
        assertThat(tester.log, is("12"));
    }

    @Test
    public void Promise_all_resolves_when_all_promises_resolve(){
        List<Promise.Defer<Integer>> deferded = createDefered(2);
        Promise.all(deferded.get(0).promise, deferded.get(1).promise)
                .then(new Promise.Handler<List<Integer>>() {
                    @Override
                    public void trigger(Promise.Triggered<List<Integer>> p) throws Exception {
                        List<Integer> result = p.getResult();
                        assertThat(result, contains(1,2));
                    }
                })
                .then(tester.handler("T"), tester.handler("F"));

        tester.assertNotInvoked();
        deferded.get(0).resolveWithResult(1);
        tester.assertNotInvoked();
        deferded.get(1).resolveWithResult(2);
        tester.assertInvoked();
        assertThat(tester.log, is("T"));
    }

    @Test
    public void Promise_all_rejects_when_any_promise_rejects(){
        List<Promise.Defer<Integer>> deferded = createDefered(2);
        Promise.all(deferded.get(0).promise, deferded.get(1).promise).then(tester.handler("T"), tester.handler("F"));
        tester.assertNotInvoked();
        deferded.get(0).resolveWithResult(1);
        tester.assertNotInvoked();
        deferded.get(1).rejectWithException(new Exception());
        tester.assertInvoked();
        assertThat(tester.log, is("F"));
    }

    @Test
    public void Promise_all_rejects_only_once_even_when_multiple_rejects_occur(){
        List<Promise.Defer<Integer>> deferded = createDefered(2);
        Promise.all(deferded.get(0).promise, deferded.get(1).promise).then(tester.handler("T"), tester.handler("F"));
        tester.assertNotInvoked();
        deferded.get(0).rejectWithException(new Exception());
        tester.assertInvoked();
        tester.wasInvoked = false;
        deferded.get(1).rejectWithException(new Exception());
        tester.assertNotInvoked();
        assertThat(tester.log, is("F"));
    }

    @Test
    public void Promise_all_resolves_and_returns_the_correct_order_of_results(){
        List<Promise.Defer<Integer>> deferded = createDefered(2);
        Promise.all(deferded.get(0).promise, deferded.get(1).promise)
                .then(new Promise.Handler<List<Integer>>() {
                    @Override
                    public void trigger(Promise.Triggered<List<Integer>> p) throws Exception {
                        List<Integer> result = p.getResult();
                        assertThat(result, contains(2,1));
                    }
                }).any(tester.handler());
        deferded.get(0).resolveWithResult(2);
        deferded.get(1).resolveWithResult(1);
        tester.assertInvoked();
    }

    @Test
    public void Promise_can_then_a_defer_block_and_trasfer_state_to_it(){
        Promise.Defer middleDefer = Promise.defer();
        middleDefer.then(tester.handler("M"));
        deferred.promise.then(middleDefer).then(tester.handler("E"));
        deferred.resolveWithResult(1);
        assertThat(tester.log, is("ME"));
    }

    @Test
    public void Promise_can_then_a_promise_and_process_failed_inside(){
        Promise.Defer middleDefer = Promise.defer();
        middleDefer.fail(tester.handler("M"));
        deferred.promise.then(middleDefer).fail(tester.handler("E"));
        deferred.rejectWithException(new Exception());
        assertThat(tester.log, is("ME"));
    }

    @Test
    public void Filter_can_translate_a_type_to_a_different_type_in_promise_flow(){
        deferred.promise.filter(new Promise.Filter<Integer, String>() {
            @Override
            public void filter(Promise.Defer<String> defer, boolean successful, Integer result, Exception exception) {
                defer.resolveWithResult("Filtered:" + result);
            }
        }).then(tester.logString());
        deferred.resolveWithResult(1);
        assertThat(tester.log, is("Filtered:1"));
    }

    @Test
    public void ToString_filter_converts_Object_to_string(){
        deferred.promise.filter(Promise.<Integer>toStringFilter()).then(tester.logString());
        deferred.resolveWithResult(123);
        assertThat(tester.log, is("123"));
    }

    @Test
    public void Defer_execution_of_promise(){
        deferred.promise.defer(new Promise.DeferBlock<Integer>() {
            @Override
            public void trigger(Promise.Defer<Integer> defer, Promise.Triggered<Integer> p) throws Exception {
                defer.resolveWithResult(2);
            }
        }).then(new Promise.Handler<Integer>() {
            @Override
            public void trigger(Promise.Triggered<Integer> p) throws Exception {
                tester.invoked(p.getResult().toString());
            }
        });
        deferred.resolveWithResult(1);
        assertThat(tester.log, is("2"));
    }

    @Test
    public void Defer_execution_catch_exception(){
        deferred.promise.defer(new Promise.DeferBlock<Integer>() {
            @Override
            public void trigger(Promise.Defer<Integer> defer, Promise.Triggered<Integer> p) throws Exception {
                throw new Exception("Catch");
            }
        }).fail(tester.logException());
        deferred.resolveWithResult(1);
        assertThat(tester.log, is("Catch"));
    }

    @Test
    public void Promise_trigger_should_keep_result_alive_until_deallocated(){
        final Promise.Triggered<Integer>[] triggerCopy = new Promise.Triggered[1];
        deferred.promise.then(new Promise.Handler<Integer>() {
            @Override
            public void trigger(Promise.Triggered<Integer> p) throws Exception {
                triggerCopy[0] = p;
            }
        });
        deferred.resolveWithResult(1);
        assertThat(triggerCopy[0].getResult(), is(not(nullValue())));
    }

    private <T> List<Promise.Defer<T>> createDefered(int count){
        List<Promise.Defer<T>> deferList = new LinkedList<>();
        while ( count-- > 0 ){
            deferList.add(Promise.<T>defer());
        }
        return deferList;
    }

    static class ThreadTesting{
        public Promise.Defer deferred;
        private List<Thread> threads = new LinkedList<>();

        public void startThread(Runnable runnable){
            startThread(1, runnable);
        }

        public void startThread(int count, Runnable runnable){
            while ( count-- > 0 ) {
                Thread thread = new Thread(runnable);
                threads.add(thread);
                thread.start();
            }
        }

        public void join(long ms){
            boolean alive;
            do {
                alive = false;
                Thread.yield();
                for (Thread thread: threads) {
                    if ( thread.isAlive() ) {
                        alive = true;
                        break;
                    }
                }
            } while(alive);
        }
    }

    private void assertSuccessHandlerInvoked(Promise.Defer p, PromiseTester trigger) throws Exception {
        trigger.assertNotInvoked();
        p.resolveWithResult(123);
        trigger.assertInvoked();
    }

    private void assertFailureHandlerInvoked(Promise.Defer p, PromiseTester trigger) throws Exception {
        trigger.assertNotInvoked();
        p.rejectWithException(new Exception());
        trigger.assertInvoked();
    }

    static class PromiseTester {
        boolean wasInvoked = false;
        String log = "";

        void invoked(String log){
            this.log += log;
            this.wasInvoked = true;
        }

        void invoked(){
            invoked("");
        }

        Promise.Handler<String> logString() {
            return new Promise.Handler<String>() {
                @Override
                public void trigger(Promise.Triggered<String> p) throws Exception {
                    invoked(p.getResult());
                }
            };
        }

        Promise.Handler handler(){
            return handler("");
        }

        Promise.Handler handler(final String log){
            return new Promise.Handler(){
                @Override
                public void trigger(Promise.Triggered p) {
                    invoked(log);
                }
            };
        }

        Promise.Handler logException(){
            return new Promise.Handler() {
                @Override
                public void trigger(Promise.Triggered p) throws Exception {
                    //noinspection ThrowableResultOfMethodCallIgnored
                    invoked(p.getException().getMessage());
                }
            };
        }

        void assertNotInvoked(){
            assertThat(wasInvoked, is(false));
        }

        void assertInvoked(){
            assertThat(wasInvoked, is(true));
        }
    }

}