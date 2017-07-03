package com.threeplay.core;

import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNot.not;

public class FnTest {

    private List<Integer> list;

    @Before
    public void setup_list(){
        list= new LinkedList<>();
        list.add(1); list.add(5); list.add(2); list.add(3); list.add(10); list.add(15);
    }

    @Test
    public void convert_a_list_to_functional_object(){
        assertThat(Fn.from(list), is(not(nullValue())));
    }

    @Test
    public void can_return_a_list_from_functional_object(){
        assertThat(Fn.from(list).toList(), contains(1,5,2,3,10,15));
    }

    @Test
    public void can_filter_values_according_to_predicate(){
        assertThat(Fn.from(list).filter(Fn.<Integer>oddNumbers()).toList(), contains(1,5,3,15));
    }

    @Test
    public void can_map_values_from_one_type_to_another(){
        assertThat(Fn.from(list).map(new Fn.Mapper<Integer, String>() {
            @Override
            public String fromValue(Integer value) {
                return "x" + value + "x";
            }
        }).toList(), contains("x1x", "x5x", "x2x", "x3x", "x10x", "x15x"));
    }

    @Test
    public void can_reduce_values_to_a_single_return_value(){
        assertThat(Fn.from(list).reduce(Fn.sum()), is(36));
    }

    @Test
    public void can_reduce_values_to_a_single_return_value_with_initial_value(){
        assertThat(Fn.from(list).reduce("", Fn.<Integer>concat()), is("15231015"));
    }
}
