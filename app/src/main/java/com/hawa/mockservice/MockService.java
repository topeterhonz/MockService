package com.hawa.mockservice;


import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.widget.ImageView;
import android.widget.Toast;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.mockwebserver.Dispatcher;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import okio.Buffer;

public class MockService extends Service {

    private static final int START = 0;
    private static final int BYPASS = 1;
    private static final int STOP = 2;
    private static final int SHUTDOWN = 3;


    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private MockWebServer mMockWebServer;

    private int mStartId;
    private boolean mIsStarted;
    private boolean mIsBypass;

    private String mHost = "http://api.tumblr.com";

    private ImageView mChatHead;


    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.arg1) {
                case START:
                    startMockService();
                    break;

                case BYPASS:
                    bypass();
                    break;

                case SHUTDOWN:
                    stopSelf(mStartId);
                    break;

            }

            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job

        }
    }

    private void bypass() {
        mIsBypass = true;
    }

    private Dispatcher mDispatcher = new Dispatcher() {
        @Override
        public MockResponse dispatch(RecordedRequest request) throws InterruptedException {

            if (mIsBypass) {
                return getRequest(request);
            } else {
                return new MockResponse()
                        .setResponseCode(200)
                        .setBody("hello");
            }
        }
    };

    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");


    private MockResponse getRequest(RecordedRequest mockRequest) {
        MockResponse mockResponse = new MockResponse();

        try {

            HttpRequestBase request = null;
            String url = mHost + mockRequest.getPath();
            if (mockRequest.getMethod().equals("GET")) {
                request = new HttpGet(url);
            } else if (mockRequest.getMethod().equals("POST")) {
                request = new HttpPost(url);
            }
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response = httpclient.execute(request);

            StatusLine statusLine = response.getStatusLine();
            mockResponse.setResponseCode(statusLine.getStatusCode());

            if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                Buffer buffer = new Buffer();
                buffer.readFrom(response.getEntity().getContent());
                mockResponse.setBody(buffer);
            } else{
                //Closes the connection.
                response.getEntity().getContent().close();
                throw new IOException(statusLine.getReasonPhrase());
            }


        } catch (Exception e) {
        }

        return mockResponse;
    }

    private void startMockService() {
        mMockWebServer = new MockWebServer();
        mMockWebServer.setDispatcher(mDispatcher);

        try {
            mMockWebServer.start(13579);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onCreate() {
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);

        mChatHead = new ImageView(this);

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        mStartId = startId;

        if (!mIsStarted) {
            mIsStarted = true;
            Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        }


        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mServiceHandler.obtainMessage();

        switch (intent.getStringExtra("Param")) {
            case "Start":
                msg.arg1 = START;
                break;

            case "Bypass":
                msg.arg1 = BYPASS;
                break;
        }

        mServiceHandler.sendMessage(msg);

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }
}
