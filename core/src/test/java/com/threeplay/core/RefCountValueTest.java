package com.threeplay.core;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Created by eliranbe on 1/31/17.
 */

public class RefCountValueTest {

    RefCountValue<Integer> refCountValue;
    @Before
    public void setup(){
        refCountValue = new RefCountValue<>();
    }

    @Test
    public void value_can_be_set(){
        assertThat(refCountValue.set(1), is(true));
    }

    @Test
    public void value_can_only_be_set_once(){
        refCountValue.set(1);
        assertThat(refCountValue.set(1), is(false));
    }

    @Test
    public void value_can_be_released_to_be_set_again(){
        refCountValue.set(1);
        refCountValue.release();
        assertThat(refCountValue.set(1), is(true));
    }

    @Test
    public void retain_with_no_value_returns_null(){
        assertThat(refCountValue.retain(), is(nullValue()));
    }

    @Test
    public void retain_with_value_returns_that_value(){
        refCountValue.set(1);
        assertThat(refCountValue.retain(), is(1));
    }

    @Test
    public void retain_must_be_released_before_value_can_be_set(){
        refCountValue.set(1);
        refCountValue.retain();
        refCountValue.release();
        assertThat(refCountValue.set(1), is(false));
        refCountValue.release();
        assertThat(refCountValue.set(1), is(true));
    }

    @Test
    public void release_returns_null_when_not_the_last_release(){
        assertThat(refCountValue.release(), is(nullValue()));
        refCountValue.set(1);
        refCountValue.retain();
        assertThat(refCountValue.release(), is(nullValue()));
    }

    @Test
    public void last_release_returns_true(){
        refCountValue.set(1);
        assertThat(refCountValue.release(), is(1));
        refCountValue.set(2);
        refCountValue.retain();
        refCountValue.release();
        assertThat(refCountValue.release(), is(2));
    }

    @Test
    public void get_returns_null_if_no_value_is_set_or_is_released(){
        assertThat(refCountValue.get(), is(nullValue()));
        refCountValue.set(1);
        refCountValue.release();
        assertThat(refCountValue.get(), is(nullValue()));
    }

    @Test
    public void get_returns_the_set_value(){
        refCountValue.set(1);
        assertThat(refCountValue.get(), is(1));
        refCountValue.retain();
        assertThat(refCountValue.get(), is(1));
    }

}
