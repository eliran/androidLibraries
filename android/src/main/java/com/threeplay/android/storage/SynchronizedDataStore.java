package com.threeplay.android.storage;

import android.util.Log;

import com.threeplay.android.events.EventSource;
import com.threeplay.android.fetcher.PersistentFetcher;
import com.threeplay.android.fetcher.ResourceFetchManager;
import com.threeplay.core.Promise;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by eliranbe on 7/1/17.
 */

public class SynchronizedDataStore {
//    private static final String TAG = "SyncRemoteDataStore";
//    private static final String INDEX = "__index__";
//    private final DataStore dataStore;
//    private final DataSourceManager manager;
//    private WeakReference<Listener> listener = new WeakReference<>(null);
//    private int dataRevision = 0;
//
//    private final String configurationKey;
//    private final PersistentFetcher<InputStream> configurationFetcher;
//    private final ResourceFetchManager<InputStream> fetchManager;
//    private boolean downloadAllAssets = false;
//
//    public interface Listener {
//        void dataStoreUpdateComplete(SynchronizedRemoteDataStore dataStore, boolean successful);
//    }
//
//    interface DataSourceManager {
//        interface DataItem {
//        }
//        Promise<List<String>> fetchIndex();
//        EventSource<DataItem> fetchGroup(List<String> files);
//    }
//
//    public SynchronizedDataStore(DataSourceManager manager, DataStore dataStore){
//        this.manager = manager;
//        this.configurationFetcher = new PersistentFetcher<>(resourceFetcher, configurationKey).setListener(this);
//        this.fetchManager = new ResourceFetchManager<>(5, resourceFetcher);
//        this.dataStore = dataStore;
//        startupLoadConfiguration();
//    }
//
//    public void setListener(Listener listener){
//        this.listener = new WeakReference<>(listener);
//    }
//
//    public int getRevision() {
//        return dataRevision;
//    }
//
//    public void refresh(boolean full){
//        manager.fetchIndex().then(indexFetchedHandler());
//        configurationFetcher.start();
//    }
//
//    private Promise.Handler<List<String>> indexFetchedHandler(){
//        return new Promise.Handler<List<String>>() {
//            @Override
//            public void trigger(Promise.Triggered<List<String>> p) throws Exception {
//                writeIndex(p.getResult());
//                processIndex();
//            }
//        };
//    }
//
//    private void startupLoadConfiguration(){
//        if ( dataStore.contains(INDEX) ) {
//            processIndex();
//        }
//        else {
//            refresh(true);
//        }
//    }
//
//    private void processIndex(){
//        List<String> files = keepNewResources(readIndex(dataStore.getStream(INDEX)));
//        if ( files.size() > 0 ) {
//            manager.fetchGroup(files).subscribe();
//        }
//    }
//
//    private ResourceFetchManager.FetchHandler<InputStream> storeResourcesHandler() {
//        return new ResourceFetchManager.FetchHandler<InputStream>() {
//            @Override public void onBeginFetch(String key) {}
//
//            @Override
//            public void onContent(String key, InputStream content, Promise.Defer<InputStream> defer) throws Exception {
//                dataStore.store(key, content);
//                defer.resolveWithResult(null);
//            }
//
//            @Override public void onEndFetch(String key, boolean successful) {}
//
//            @Override
//            public void onComplete(int total, int failed, int canceled) {
//                Log.d(TAG, "sync complete with " + total + " files and " + failed + " failed");
//                Listener _listener = listener.get();
//                if ( _listener != null ) {
//                    _listener.dataStoreUpdateComplete(SynchronizedRemoteDataStore.this, true);
//                }
//            }
//        };
//    }
//
//    private void addDownloadableResources(List<String> list, List<String> resources){
//        list.addAll(downloadList(resources));
//    }
//
//    private List<String> downloadList(List<String> resources){
//        if ( downloadAllAssets ) {
//            return resources;
//        }
//        List<String> filteredList = new LinkedList<>();
//        for (String resource: resources) {
//            if ( !dataStore.contains(resource) ) {
//                filteredList.add(resource);
//            }
//        }
//        return filteredList;
//    }
//
}
