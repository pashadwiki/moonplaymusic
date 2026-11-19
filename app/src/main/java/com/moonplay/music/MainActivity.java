package com.moonplay.music;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * MoonPlay Music - a Spotify-style music player.
 * The whole UI lives in assets/index.html (WebView). This activity provides
 * the native pieces a web page cannot do alone: real file downloads through
 * DownloadManager and opening downloaded songs with the system player.
 */
public class MainActivity extends Activity {

    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new MoonBridge(), "MoonBridge");
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition,
                                        String mimeType, long contentLength) {
                enqueueDownload(url, null);
            }
        });

        if (Build.VERSION.SDK_INT < 29
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 7);
        }

        webView.loadUrl("file:///android_asset/index.html");
    }

    /** Enqueue a song download into the public Download/MoonPlay folder. */
    private long enqueueDownload(String url, String fileName) {
        try {
            if (fileName == null || fileName.trim().isEmpty()) {
                fileName = Uri.parse(url).getLastPathSegment();
            }
            if (fileName == null || fileName.trim().isEmpty()) {
                fileName = "moonplay-track.mp3";
            }
            fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle(fileName);
            request.setDescription("MoonPlay Music");
            request.setMimeType("audio/mpeg");
            request.setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS, "MoonPlay/" + fileName);

            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            long id = dm.enqueue(request);
            Toast.makeText(this, "Download dimulai: " + fileName, Toast.LENGTH_SHORT).show();
            return id;
        } catch (Exception e) {
            Toast.makeText(this, "Download gagal: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return -1;
        }
    }

    /** List finished downloads (title + local uri) as a JSON array string. */
    private String listDownloads() {
        JSONArray result = new JSONArray();
        try {
            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Query query = new DownloadManager.Query()
                    .setFilterByStatus(DownloadManager.STATUS_SUCCESSFUL);
            Cursor cursor = dm.query(query);
            if (cursor != null) {
                int titleCol = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE);
                int uriCol = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                int descCol = cursor.getColumnIndex(DownloadManager.COLUMN_DESCRIPTION);
                while (cursor.moveToNext()) {
                    String desc = descCol >= 0 ? cursor.getString(descCol) : null;
                    if (!"MoonPlay Music".equals(desc)) {
                        continue; // only show songs downloaded by this app
                    }
                    JSONObject item = new JSONObject();
                    item.put("title", titleCol >= 0 ? cursor.getString(titleCol) : "Lagu");
                    item.put("uri", uriCol >= 0 ? cursor.getString(uriCol) : "");
                    result.put(item);
                }
                cursor.close();
            }
        } catch (Exception ignored) {
        }
        return result.toString();
    }

    /** Open a downloaded song with the device's music player. */
    private void openDownloaded(String uriString) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(uriString), "audio/*");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Tidak bisa membuka file ini", Toast.LENGTH_LONG).show();
        }
    }

    /** JavaScript bridge exposed to the player UI as window.MoonBridge. */
    public class MoonBridge {
        @JavascriptInterface
        public long download(String url, String fileName) {
            return enqueueDownload(url, fileName);
        }

        @JavascriptInterface
        public String getDownloads() {
            return listDownloads();
        }

        @JavascriptInterface
        public void openFile(String uri) {
            openDownloaded(uri);
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
