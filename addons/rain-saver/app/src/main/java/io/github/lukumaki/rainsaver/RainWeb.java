package io.github.lukumaki.rainsaver;

import android.content.Context;
import android.graphics.Color;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.Locale;

/**
 * The rainy city as a WebView: the page in assets/rain, drawn by the TV's own WebView. The page is read from the
 * app's own files and the app has no INTERNET permission, so nothing can be loaded from the network.
 */
final class RainWeb {
    private RainWeb() {
    }

    /** A black, script-only WebView with no links followed; {@code onGone} runs if its renderer is killed. */
    static WebView create(Context ctx, Runnable onGone) {
        WebView web = new WebView(ctx);
        web.setBackgroundColor(Color.BLACK);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(false);
        s.setAllowFileAccess(false);            // android_asset URLs are read regardless
        s.setAllowContentAccess(false);
        s.setBlockNetworkLoads(true);
        s.setSupportZoom(false);
        s.setTextZoom(100);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return true;
            }

            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                // The renderer was killed (low memory) or crashed: end the screen saver instead of taking the app down.
                onGone.run();
                return true;
            }
        });
        return web;
    }

    /** The page with the add-on's options (res/values/options.xml) and the TV's language in its address. */
    static String url(Context ctx) {
        return url(ctx, -1);
    }

    /** The same, with the frame cap given (a test from the preview's launch command); below 0 keeps the option. */
    static String url(Context ctx, int fpsOverride) {
        return "file:///android_asset/rain/index.html?view=" + ctx.getString(R.string.rain_view)
                + "&h24=" + (ctx.getResources().getBoolean(R.bool.clock_24h) ? "1" : "0")
                + "&lang=" + Locale.getDefault().toLanguageTag()
                + "&fps=" + (fpsOverride >= 0 ? fpsOverride : ctx.getResources().getInteger(R.integer.rain_fps));
    }
}
