package com.threeplay.android;

import android.os.AsyncTask;
import android.util.Log;

import com.threeplay.core.Promise;
import com.threeplay.core.QUtils;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by eliranbe on 1/4/17.
 */
public class HttpClient {
    private static final String TAG = "HttpClient";
    private static final boolean WITH_LOG = true;

    private static final String CREATED = "created";
    private static final String SENDING = "sending";
    private static final String COMPLETED = "completed";
    private static final String FAILURE = "failure";

    private final AtomicInteger connections = new AtomicInteger(0);


    public static class Response {
        private final byte[] responseContent;
        private final String contentType;
        private final Map<String, String> headers;
        private final int httpStatus;
        private final long elapsed;

        public static Response build(HttpURLConnection conn, InputStream is, long elapsed){
            Map<String, String> headers = new HashMap<>();
            int i = 1;
            do {
                String header = conn.getHeaderFieldKey(i);
                if (header == null) break;
                headers.put(header, conn.getHeaderField(i));
            } while ( ++i < 32 );
            int httpStatus = 0;
            try { httpStatus = conn.getResponseCode(); } catch ( IOException ok ) {}
            return new Response(is, httpStatus, conn.getContentType(), headers, elapsed);
        }

        private Response(InputStream is, int httpStatus, String contentType, Map<String, String> headers, long elapsed){
            this.responseContent = blockReadStream(is);
            this.httpStatus = httpStatus;
            this.contentType = contentType;
            this.headers = headers;
            this.elapsed = elapsed;
        }

        public int getHttpStatus() {
            return httpStatus;
        }

        public byte[] content(){
            return responseContent;
        }

        private byte[] blockReadStream(InputStream is){
            final ByteArrayOutputStream bf = new ByteArrayOutputStream();
            final byte[] readBuffer = new byte[1024];
            do {
                try {
                    int byteRead = is.read(readBuffer);
                    if (byteRead == -1) break;
                    bf.write(readBuffer, 0, byteRead);
                } catch ( IOException e ) {
                    break;
                }
            } while ( true );
            return bf.toByteArray();
        }

        public String contentType() {
            return contentType;
        }

        public int contentLength() {
            return responseContent.length;
        }

        public boolean isContentTypeJSON(){
            return "application/json".equals(contentType());
        }

        public String getHeader(String key){
            return headers.get(key);
        }

        public Set<String> headerKeys(){
            return headers.keySet();
        }

        public JSONObject json() {
            return QUtils.jsonFromString(new String(responseContent));
        }

    }

    public static class Request {
        private final String method;
        private final URL path;
        private String contentType;
        private byte[] content;
        private boolean logRequest = WITH_LOG;
        private String state = CREATED;
        private Map<String, String> requestProperties = null;
        private HttpClient client;

        public Request(HttpClient client, String method, URL path){
            this.method = method;
            this.path = path;
            this.client = client;
        }

        public Request setContentType(String contentType){
            this.contentType = contentType;
            return this;
        }

        public Request setContent(byte[] content){
            this.content = content;
            return this;
        }

        public Request setIfNoneMatch(String etag){
            return setRequestProperty("If-None-Match", etag);
        }

        private Request setRequestProperty(String key, String value) {
            if ( requestProperties == null ) {
                requestProperties = new HashMap<>();
            }
            if ( value == null ) {
                requestProperties.remove(key);
            }
            else {
                requestProperties.put(key, value);
            }
            return this;
        }

        public Request setContent(String contentType, byte[] content){
            return setContentType(contentType).setContent(content);
        }

        public Promise<Response> send(String contentType, byte[] content){
            return setContent(contentType, content).send();
        }

        public Promise<Response> send(){
            final Promise.Defer<Response> defer = Promise.defer();
            state = SENDING;
            try {
                client.requestStarts();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        long startTime = System.currentTimeMillis();
                        HttpURLConnection conn = null;
                        try {
                            conn = (HttpURLConnection) path.openConnection();
                            conn.setRequestMethod(method);
                            if (contentType != null) {
                                conn.setRequestProperty("Content-Type", contentType);
                            }
                            if ( requestProperties != null ) {
                                for (Map.Entry<String, String> property : requestProperties.entrySet()) {
                                    conn.setRequestProperty(property.getKey(), property.getValue());
                                }
                            }
                            conn.setDoInput(true);
                            if (content != null) {
                                conn.setDoOutput(true);
                                OutputStream os = new DataOutputStream(conn.getOutputStream());
                                os.write(content);
                                os.flush();
                                os.close();
                            }
                            state = COMPLETED;
                            defer.resolveWithResult(Response.build(conn, new BufferedInputStream(conn.getInputStream()), System.currentTimeMillis() - startTime));
                            client.requestEnded(false);
                        } catch (Exception e) {
                            client.requestEnded(true);
                            state = FAILURE;
                            defer.rejectWithException(e);
                        } finally {
                            content = null;
                            if (conn != null) {
                                conn.disconnect();
                            }
                            conn = null;
                        }
                    }
                }).start();
            } catch ( Exception e ) {
                content = null;
                state = FAILURE;
                defer.rejectWithException(e);
            }
            return logRequest ? logResponse(method, path, defer.promise) : defer.promise;
        }

        private Promise<Response> logResponse(final String prefix, final URL path, Promise<Response> responsePromise){
            return responsePromise.then(new Promise.Handler<Response>() {
                @Override
                public void trigger(Promise.Triggered<Response> p) throws Exception {
                    Response response = p.getResult();
                    Log.i(TAG, prefix + " Response: " + response.contentType() + " length " + response.contentLength() + " elapsed " + response.elapsed + " path " + path );
                }
            }).fail(new Promise.Handler<Response>() {
                @Override
                public void trigger(Promise.Triggered<Response> p) throws Exception {
                    Log.e(TAG, prefix + " Failed: " + p.getException().getLocalizedMessage());
                }
            });
        }

    }

    private final URL baseURL;

    public HttpClient(String baseURL) throws MalformedURLException {
        this(new URL(baseURL));
    }

    public HttpClient(URL baseURL) {
        this.baseURL = baseURL;
    }

    public Integer pendingConnections(){
        return connections.get();
    }

    public Promise<Response> PUT(String relativeURL, byte[] content) {
        return sendHttpRequest("PUT", relativeURL, "application/octet-stream", content, WITH_LOG);
    }

    public Promise<Response> PUT(String relativeURL) {
        return sendHttpRequest("PUT", relativeURL, null, null, WITH_LOG);
    }

    public Promise<Response> PUT(String relativeURL, JSONObject json) {
        return sendHttpRequest("PUT", relativeURL, "application/json", json.toString().getBytes(), WITH_LOG);
    }

    public Promise<Response> POST(String relativeURL, byte[] content) {
        return POST(relativeURL, "application/octet-stream", content);
    }

    public Promise<Response> POST(String relativeURL, String contentType, byte[] content) {
        return sendHttpRequest("POST", relativeURL, contentType, content, WITH_LOG);
    }

    public Promise<Response> GET(String relativeURL) {
        return sendHttpRequest("GET", relativeURL, null, null, WITH_LOG);
    }

    public Promise<Response> DELETE(String relativeURL) {
        return sendHttpRequest("DELETE", relativeURL, null, null, WITH_LOG);
    }

    public Request buildGET(String relativeURL){
        return new Request(this, "GET", getPath(relativeURL));
    }

    private URL getPath(String relativePath){
        try {
            return relativePath != null ? new URL(this.baseURL, relativePath) : this.baseURL;
        } catch (Exception e) {
            return null;
        }
    }

    private Promise<Response> sendHttpRequest(final String method, final String relativePath, final String contentType, final byte[] content, boolean withLog) {
        try {
            return new Request(this, method, relativePath != null ? new URL(this.baseURL, relativePath) : this.baseURL).send(contentType, content);
        } catch (Exception e) {
            return Promise.withException(e);
        }
    }

    void requestStarts(){
        Log.d(TAG, "Start request: " + connections.incrementAndGet());
    }

    void requestEnded(boolean failure){
        Log.d(TAG, "End request: " + connections.decrementAndGet());
    }
}
