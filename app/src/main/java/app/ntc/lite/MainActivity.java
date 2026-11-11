package app.ntc.lite;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.webkit.CookieManager;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.window.OnBackInvokedDispatcher;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    private static final String HOME_URL = "https://ntc.party/";

    // true  = со всех сторонних сайтов разрешены только картинки (максимум приватности,
    //         но форум может сломаться, если берёт скрипты или стили с чужого CDN).
    // false = блокируются только известные трекеры и реклама (безопасный вариант).
    private static final boolean STRICT_THIRD_PARTY = false;

    // Свои домены сайта: им можно всё.
    private static final String[] SITE_HOSTS = {"ntc.party"};

    // Известные трекеры и рекламные сети: запросы к ним вырезаются всегда.
    private static final String[] BLOCKED_HOSTS = {
            "mc.yandex.ru", "mc.yandex.com", "an.yandex.ru", "yandexadexchange.net",
            "google-analytics.com", "googletagmanager.com", "googlesyndication.com",
            "googleadservices.com", "doubleclick.net",
            "top-fwz1.mail.ru", "top.mail.ru", "counter.yadro.ru", "tns-counter.ru",
            "adfox.ru", "adriver.ru", "mytracker.ru", "criteo.com", "hotjar.com",
            "facebook.net", "connect.facebook.net", "clarity.ms", "scorecardresearch.com",
            "adnxs.com", "taboola.com", "outbrain.com", "mixpanel.com", "amplitude.com",
            "segment.io", "segment.com"
    };

    private WebView web;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Приложение рисуется под системными панелями (edge-to-edge), поэтому добавляем отступы.
        FrameLayout root = new FrameLayout(this);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsets.Type.systemBars()
                            | WindowInsets.Type.displayCutout()
                            | WindowInsets.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsets.CONSUMED;
        });

        web = new WebView(this);
        root.addView(web, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);          // форуму нужен JS
        s.setDomStorageEnabled(true);          // и localStorage
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setGeolocationEnabled(false);
        s.setSupportMultipleWindows(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(web, false); // сторонние куки выключены

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // Встроенные фреймы (например, видео) не трогаем.
                if (!request.isForMainFrame()) return false;
                Uri uri = request.getUrl();
                if (isSite(uri)) return false;
                openExternal(uri);
                return true;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (request.isForMainFrame()) return null;
                String host = request.getUrl().getHost();
                if (host == null) return null;
                if (matches(host, BLOCKED_HOSTS)) return blockedResponse();
                if (STRICT_THIRD_PARTY
                        && !matches(host, SITE_HOSTS)
                        && !looksLikeImage(request)) {
                    return blockedResponse();
                }
                return null;
            }

            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                recreate();
                return true;
            }
        });

        // Кнопка/жест «назад»: сначала по истории страниц, потом выход.
        getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                () -> {
                    if (web.canGoBack()) {
                        web.goBack();
                    } else {
                        finish();
                    }
                });

        if (savedInstanceState != null) {
            web.restoreState(savedInstanceState);
        } else {
            web.loadUrl(HOME_URL);
        }
    }

    private static boolean isSite(Uri uri) {
        String host = uri.getHost();
        if (host == null || !"https".equals(uri.getScheme())) return false;
        return matches(host, SITE_HOSTS);
    }

    private static boolean matches(String host, String[] domains) {
        host = host.toLowerCase(Locale.ROOT);
        for (String d : domains) {
            if (host.equals(d) || host.endsWith("." + d)) return true;
        }
        return false;
    }

    private static boolean looksLikeImage(WebResourceRequest request) {
        for (Map.Entry<String, String> h : request.getRequestHeaders().entrySet()) {
            if ("Accept".equalsIgnoreCase(h.getKey()) && h.getValue() != null
                    && h.getValue().startsWith("image/")) {
                return true;
            }
        }
        String path = request.getUrl().getPath();
        if (path == null) return false;
        path = path.toLowerCase(Locale.ROOT);
        return path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg")
                || path.endsWith(".gif") || path.endsWith(".webp") || path.endsWith(".svg")
                || path.endsWith(".avif") || path.endsWith(".ico");
    }

    private static WebResourceResponse blockedResponse() {
        return new WebResourceResponse("text/plain", "utf-8", 403, "Blocked",
                new HashMap<>(), new ByteArrayInputStream(new byte[0]));
    }

    private void openExternal(Uri uri) {
        String scheme = uri.getScheme();
        if (!"https".equals(scheme) && !"http".equals(scheme)) return;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException ignored) {
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    @Override
    protected void onPause() {
        web.onPause();
        CookieManager.getInstance().flush(); // сохраняем вход в аккаунт
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
    }

    @Override
    protected void onDestroy() {
        ViewGroup parent = (ViewGroup) web.getParent();
        if (parent != null) parent.removeView(web);
        web.destroy();
        super.onDestroy();
    }
}
