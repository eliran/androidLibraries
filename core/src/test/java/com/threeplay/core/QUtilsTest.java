package com.threeplay.core;

import org.json.JSONObject;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.number.IsCloseTo.closeTo;

/**
 * Created by eliranbe on 5/18/16.
 */
public class QUtilsTest {

    @Test
    public void join_strings(){
        assertThat(QUtils.joinStrings("a","b","c"), is("abc"));
        assertThat(QUtils.joinStrings(Arrays.asList(new String[]{"a", "c", "b"})), is("acb"));
    }

    @Test
    public void join_componenets(){
        assertThat(QUtils.joinComponents(",","a","b","c"), is("a,b,c"));
        assertThat(QUtils.joinComponents(":", Arrays.asList(new String[]{"a","c","b"})),is("a:c:b"));
    }

    @Test
    public void parseFloats() {
        float[] floats = QUtils.parseFloats("1.0,5.2,3,1");
        assertThat(floats.length, is(4));
        assertThat((double) floats[0], closeTo(1.0, 0.1));
        assertThat((double) floats[1], closeTo(5.2, 0.1));
        assertThat((double) floats[2], closeTo(3.0, 0.1));
        assertThat((double) floats[3], closeTo(1.0, 0.1));
    }

    @Test
    public void upcaseCamel(){
        assertThat(QUtils.upcaseCamel("testing-upcase-camel"), is("TestingUpcaseCamel"));
    }

    @Test
    public void downcaseCamel(){
        assertThat(QUtils.downcaseCamel("TESTING-DOWNCASE-CAMEL"), is("testingDowncaseCamel"));
    }

    @Test
    public void downcaseCamelBuilder(){
        StringBuilder builder = new StringBuilder();
        QUtils.builderWithDowncaseCamel(builder, "test-camel");
        assertThat(builder.toString(), is("testCamel"));
    }

    @Test
    public void upcaseCamelBuilder(){
        StringBuilder builder = new StringBuilder();
        QUtils.builderWithUpcaseCamel(builder, "test-camel");
        assertThat(builder.toString(), is("TestCamel"));
    }

    @Test
    public void get_returns_map_value(){
        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        assertThat(QUtils.get(map, "key", "other"), is("value"));
    }

    @Test
    public void get_returns_default_value(){
        Map<String, String> map = new HashMap<>();
        assertThat(QUtils.get(map, "key", "other"), is("other"));
    }

    @Ignore
    public void convert_string_to_json_object(){
        JSONObject json = QUtils.jsonFromString("{\"array\": [], \"string\": \"string\", \"int\": 1}");
        assertThat(json, is(not(nullValue())));
        assertThat(json.optJSONArray("array"), is(not(nullValue())));
        assertThat(json.optString("string"), is("string"));
        assertThat(json.optInt("int"), is(1));
    }

    @Test
    public void declaredMethod_privateMethod(){
        Method m = QUtils.declaredMethod(ReflectionDummy.class, "privateMethod", int.class);
        assertThat(m, is(not(nullValue())));
    }

    @Test
    public void declaredMethod_noSuchMethod(){
        Method m = QUtils.declaredMethod(ReflectionDummy.class, "noSuchMethod");
        assertThat(m, is(nullValue()));
    }

    @Test
    public void method_returns_null_for_private_methods(){
        Method m = QUtils.method(ReflectionDummy.class, "privateMethod", int.class);
        assertThat(m, is(nullValue()));
    }

    @Test
    public void method_returns_for_public_methods(){
        Method m = QUtils.method(ReflectionDummy.class, "publicMethod", int.class);
        assertThat(m, is(not(nullValue())));
    }

    @Test
    public void stringFromStream_null_stream_returns_empty_string(){
        assertThat(QUtils.stringFromStream(null), is(""));
    }

    @Test
    public void stringFromStream_returns_string(){
        assertThat(QUtils.stringFromStream(new ByteArrayInputStream("test".getBytes())), is("test"));
    }

    static class ReflectionDummy {
        private void privateMethod(int arg1){
        }
        public void publicMethod(int arg1){
        }
    }
}
