package com.threeplay.core;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created by eliranbe on 5/4/16.
 */
public class QUtils {

    public static <T>T[] joinArrays(T value, T[] array){
        @SuppressWarnings("unchecked") T[] result = (T[])Array.newInstance(value.getClass(), array.length + 1);
        result[0] = value;
        System.arraycopy(array, 0, result, 1, array.length);
        return result;
    }

    public static <T>T[] joinArrays(T[]... arrays){
        int totalLength = 0;
        for (T[] array : arrays) {
            totalLength += array.length;
        }
        @SuppressWarnings("unchecked") T[] result = (T[])Array.newInstance(arrays.getClass().getComponentType(), totalLength);
        int dst_idx = 0, array_idx = 0;

        while ( array_idx < arrays.length ) {
            int sz = arrays[array_idx].length;
            System.arraycopy(arrays[array_idx], 0, result, dst_idx, sz);
            dst_idx += sz;
        }
        return result;
    }

    public static String joinComponents(String glue, String... components){
        return joinComponents(glue, Arrays.asList(components));
    }

    public static <T> List<List<T>> splitList(List<T> list, int maxSize){
        int count = 0;
        List<List<T>> listOfLists = new LinkedList<>();
        List<T> temp = new LinkedList<>();
        for ( T element: list ) {
            temp.add(element);
            if ( ++count >= maxSize ) {
                count = 0;
                listOfLists.add(temp);
                temp = new LinkedList<>();
            }
        }
        if ( temp.size() > 0 ) {
            listOfLists.add(temp);
        }
        return listOfLists;
    }

    public static <T extends Object> String joinComponents(String glue, List<T> components){
        StringBuilder builder = new StringBuilder();
        for (Object component: components ) {
            if ( builder.length() != 0 ) builder.append(glue);
            builder.append(component);
        }
        return builder.toString();
    }

    public static String joinStrings(String... strings){
        return joinStrings(Arrays.asList(strings));
    }

    public static String joinStrings(List<String> strings){
        StringBuilder builder = new StringBuilder();
        for (String string: strings ) {
            builder.append(string);
        }
        return builder.toString();
    }

    public static <K, V>V get(Map<K, V> map, K key, V defaultValue){
        V value = map.get(key);
        if ( value == null ) return defaultValue;
        return value;
    }

    public static float[] parseFloats(String s){
        if ( s == null || s.length() == 0 ) return new float[0];

        String[] commaSeperated = s.split(",");
        float[] values = new float[commaSeperated.length];
        for ( int i = 0; i < values.length; ++i )
            values[i] = Float.valueOf(commaSeperated[i]);
        return values;
    }

    public static String upcaseCamel(String name){
        return builderWithCamel(new StringBuilder(), true, name).toString();
    }

    public static String downcaseCamel(String name){
        return builderWithCamel(new StringBuilder(), false, name).toString();
    }

    public static StringBuilder builderWithUpcaseCamel(StringBuilder builder, String name){
        return builderWithCamel(builder, true, name);
    }

    public static StringBuilder builderWithDowncaseCamel(StringBuilder builder, String name){
        return builderWithCamel(builder, false, name);
    }

    public static StringBuilder builderWithCamel(StringBuilder builder, boolean upcase, String name){
        String[] components = name.split("-");
        for (String component: components) {
            if ( upcase ) {
                builder.append(component.substring(0, 1).toUpperCase());
                builder.append(component.substring(1).toLowerCase());
            }
            else {
                builder.append((component.toLowerCase()));
                upcase = true;
            }
        }
        return builder;
    }

    @SuppressWarnings("unchecked")
    public static Method declaredMethod(Class cls, String methodName, Class... argClasses){
        try {
            return cls.getDeclaredMethod(methodName, argClasses);
        } catch ( NoSuchMethodException ok ) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static Method method(Class cls, String methodName, Class... argClasses){
        try {
            return cls.getMethod(methodName, argClasses);
        } catch ( NoSuchMethodException ok ) {
            return null;
        }
    }

    public static String stringFromStream(InputStream stream) {
        if ( stream == null ) return "";
        java.util.Scanner s = new java.util.Scanner(stream).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public static JSONObject jsonFromString(String json) {
        try {
            return new JSONObject(json);
        } catch ( JSONException|NullPointerException ok ) {
        }
        return null;
    }

    public static byte[] bytesFromStream(InputStream in){
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copyStreamTo(in, out);
        return out.toByteArray();
    }

    public static File fileFromInputStream(String fileName, InputStream in){
        File cacheFile = new File(fileName);
        if ( !cacheFile.exists() ) {
            OutputStream out = null;
            try {
                out = new FileOutputStream(cacheFile);
                copyStreamTo(in, out);
            }
            catch ( IOException e ) {
                e.printStackTrace();
            }
            finally {
                if ( in != null ) try { in.close(); } catch ( IOException ok ) {}
                if ( out != null ) try { out.close(); } catch ( IOException ok ) {}
            }
        }
        return cacheFile;
    }

    public static void copyStreamTo(InputStream in, OutputStream out) {
        final byte buf[] = new byte[16*1024];
        try {
            do {
                int bcount = in.read(buf);
                if (bcount <= 0) break;
                out.write(buf,0, bcount);
            } while(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static File copyStreamToFile(InputStream inputStream, File file) {
        OutputStream os = null;
        try {
            os = new FileOutputStream(file);
            copyStreamTo(inputStream, os);
        } catch ( FileNotFoundException ok ) {
        } finally {
            if ( os != null ) try { os.close(); } catch ( IOException ok ) {}
        }
        return file;
    }

    public static int[] uniqueValuesInRange(int minNumber, int maxNumber, int total){
        final int possibleNumbers = (maxNumber - minNumber + 1);
        if ( possibleNumbers < total ) return null;
        final int[] values = new int[possibleNumbers];
        for ( int i = 0; i < possibleNumbers; ++i ) values[i] = minNumber++;
        Random r = new Random();
        for ( int i = 0; i < possibleNumbers; ++i ) {
            int j = r.nextInt(possibleNumbers);
            if ( j != i ) {
                values[i] ^= values[j];
                values[j] ^= values[i];
                values[i] ^= values[j];
            }
        }
        return Arrays.copyOf(values, total);
    }

    public static Map<String, String> mapFromPair(String... pairs){
        final Map<String, String> map = new HashMap<>();
        int index = 0;
        while ( pairs.length - index >= 2 ) {
            map.put(pairs[index+0], pairs[index+1]);
            index += 2;
        }
        return map;
    }

    public static <T>Map<String,T> mapObjectsWithKeys(T[] objects, String... keys) {
        int count = Math.min(objects.length, keys.length);
        final Map<String, T>map = new HashMap<>();
        for ( int i = 0; i < count; ++i ) {
            map.put(keys[i], objects[i]);
        }
        return map;
    }
}
