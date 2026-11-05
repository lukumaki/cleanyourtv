package io.github.lukumaki.rainsaver;

import android.service.dreams.DreamService;
import android.view.ViewGroup;
import android.webkit.WebView;

/**
 * The screen saver (Settings > Device Preferences > Screen saver > Rain): the rainy city with the time glowing in it,
 * drifting to a new spot every 50 seconds so nothing burns in. Any button ends it; the TV still sleeps on its own
 * timer. It runs only while the screen saver shows, and uses no network.
 */
public class RainDream extends DreamService {
    private WebView web;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        web = RainWeb.create(this, this::finish);
        setContentView(web);
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        if (web == null) return;
        web.resumeTimers();
        web.loadUrl(RainWeb.url(this));
    }

    @Override
    public void onDreamingStopped() {
        if (web != null) web.loadUrl("about:blank");    // stop the animation at once, before the view is torn down
        super.onDreamingStopped();
    }

    @Override
    public void onDetachedFromWindow() {
        if (web != null) {
            if (web.getParent() instanceof ViewGroup) ((ViewGroup) web.getParent()).removeView(web);
            web.destroy();
            web = null;
        }
        super.onDetachedFromWindow();
    }
}
