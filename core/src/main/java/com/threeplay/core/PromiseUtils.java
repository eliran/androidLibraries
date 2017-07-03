package com.threeplay.core;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Created by eliranbe on 6/28/17.
 */

public class PromiseUtils {
    public static Promise.Convert<InputStream, XMLNode> inputStreamToXMLNode() {
        return new Promise.Convert<InputStream, XMLNode>() {
            @Override
            public void convert(Promise.Defer<XMLNode> defer, InputStream result) throws Exception {
                defer.resolveWithResult(XMLNode.parse(result));
            }
        };
    }

    private Promise.Convert<InputStream, byte[]> inputStreamToBytes() {
        return new Promise.Convert<InputStream, byte[]>() {
            @Override
            public void convert(Promise.Defer<byte[]> defer, InputStream result) throws Exception {
                defer.resolveWithResult(QUtils.bytesFromStream(result));
            }
        };
    }
}
