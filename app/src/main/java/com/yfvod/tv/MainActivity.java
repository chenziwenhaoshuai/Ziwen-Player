package com.yfvod.tv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.content.pm.PackageInfo;
import android.provider.Settings;
import android.text.Html;
import android.util.Base64;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;
import androidx.core.content.FileProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final String BASE_URL = "https://www.yfvod.com";
    private static final int BG = 0xFF101318;
    private static final int PANEL = 0xFF181E27;
    private static final int PANEL_2 = 0xFF222A35;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAFB8C6;
    private static final int ACCENT = 0xFFF7C843;
    private static final long SEEK_STEP_MS = 10_000L;
    private static final String MOVIE_TIME_PATH = "/vod-show/1--time---------.html";
    private static final String TV_SHOW_PATH = "/vod-show/2--time---------.html";
    private static final String VARIETY_SHOW_PATH = "/vod-show/3--time---------.html";
    private static final String ANIMATION_SHOW_PATH = "/vod-show/4--time---------.html";
    private static final String PEACH_PATH = "peach://catalog";
    private static final String PEACH_API_BASE = "https://sm-api.wieuc.com";
    private static final String PEACH_SITE_ID = "2";
    private static final String PEACH_CHANNEL_ID = "522";
    private static final String PEACH_CHANNEL_NAME = "gj-89";
    private static final int PEACH_RANDOM_PAGE_MAX = 30;
    private static final String PEACH_IMAGE_HOST = "https://hm-img.twmjjy.com";
    private static final String[] PEACH_PLAY_HOSTS = new String[]{
            "https://hm-img.twmjjy.com",
            "https://hm-vip.twmjjy.com",
            "https://hm-img.aa66cc.live"
    };
    private static final String PEACH_FERNET_KEY = "NyGRG56A8i5J2JMqh7da83r2MMfgbM7Ppw1aCF8YnAY=";
    private static final String UPDATE_API_URL = "https://api.github.com/repos/chenziwenhaoshuai/Ziwen-Player/releases/latest";
    private static final String UPDATE_APK_PREFIX = "Ziwen-Player-v";
    private static final String UPDATE_APK_SUFFIX = ".apk";
    private static final long VIDEO_CACHE_MAX_BYTES = 1024L * 1024L * 1024L;
    private static final String PREFS_NAME = "ziwen_player_settings";
    private static final String PREF_PRELOAD_MINUTES = "preload_minutes";
    private static final String PREF_RECENT_WATCHES_LEGACY = "recent_watches";
    private static final String PREF_RECENT_WATCHES = "recent_watches_v2";
    private static final String PREF_BETA_MODE = "beta_mode";
    private static final String PREF_AUTO_UPDATE_CHECK = "auto_update_check";
    private static final String PREF_LAST_AUTO_UPDATE_CHECK = "last_auto_update_check";
    private static final long AUTO_UPDATE_CHECK_INTERVAL_MS = 6L * 60L * 60L * 1000L;
    private static final int DEFAULT_PRELOAD_MINUTES = 3;
    private static final int[] PRELOAD_MINUTE_OPTIONS = new int[]{1, 2, 3, 5, 8};
    private static final int RECENT_WATCH_LIMIT = 40;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final SiteClient siteClient = new SiteClient();
    private final ImageLoader imageLoader = new ImageLoader();
    private final Random random = new Random();

    private FrameLayout root;
    private ProgressBar loading;
    private TextView toast;
    private GridView catalogGrid;
    private HorizontalScrollView sourceScroll;
    private LinearLayout sourceRow;
    private GridView episodeGrid;
    private LinearLayout navRail;
    private LinearLayout navContainer;
    private TextView activeNavButton;
    private VideoAdapter videoAdapter;
    private EpisodeAdapter episodeAdapter;
    private WebView playerWebView;
    private WebView resolverWebView;
    private ExoPlayer exoPlayer;
    private PlayerView playerView;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private SimpleCache videoCache;
    private StandaloneDatabaseProvider cacheDatabaseProvider;
    private PlayTarget pendingTarget;
    private Episode pendingEpisode;
    private boolean resolverFinished;
    private boolean playerStoppedForBackground;
    private boolean updateChecking;
    private boolean updateDownloading;
    private String updateDownloadVersion = "";
    private File updateDownloadFile;

    private String currentTitle = "首页";
    private String currentPath = "/";
    private VideoItem currentVideo;
    private int catalogPage = 1;
    private boolean catalogLoadingMore;
    private boolean catalogHasMore;
    private boolean catalogPagedMode;
    private String catalogPagingKind = "";
    private float catalogPullStartY = 0f;
    private boolean catalogPullTracking = false;
    private List<Episode> currentDetailEpisodes = new ArrayList<>();
    private List<SourceGroup> currentSources = new ArrayList<>();
    private int activeSourcePosition = 0;
    private enum Screen { CATALOG, DETAIL, PLAYER, SEARCH, SETTINGS }
    private Screen screen = Screen.CATALOG;

    private final Category[] categories = new Category[]{
            new Category("首页", "/"),
            new Category("电影", MOVIE_TIME_PATH),
            new Category("连续剧", TV_SHOW_PATH),
            new Category("综艺", VARIETY_SHOW_PATH),
            new Category("动漫", ANIMATION_SHOW_PATH),
            new Category("你懂的", PEACH_PATH)
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        keepScreenImmersive();
        root = new FrameLayout(this);
        root.setBackgroundColor(BG);
        setContentView(root);
        createLoading();
        createToast();
        clearVideoCacheOnStartup();
        showCatalog("首页", "/");
        maybeCheckUpdateOnStartup();
    }

    private void showCatalog(String title, String path) {
        if (PEACH_PATH.equals(path) && !isBetaModeEnabled()) {
            showCatalog("首页", "/");
            return;
        }
        screen = Screen.CATALOG;
        currentTitle = title;
        currentPath = path;
        currentVideo = null;
        root.removeAllViews();
        root.setBackgroundColor(BG);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.HORIZONTAL);
        page.setPadding(dp(pageHorizontalPaddingDp()), dp(pageTopPaddingDp()), dp(pageHorizontalPaddingDp()), dp(pageBottomPaddingDp()));
        root.addView(page, matchParams());

        LinearLayout left = createLeftRail(page);
        addBrand(left);

        activeNavButton = null;
        TextView search = searchNavItem(false);
        search.setOnClickListener(v -> showSearch(""));
        navContainer.addView(search, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(navItemHeightDp())));

        for (Category category : visibleCategories()) {
            boolean selected = category.path.equals(path);
            TextView item = navItem(category.name, selected);
            if (selected) {
                activeNavButton = item;
            }
            item.setOnClickListener(v -> showCatalog(category.name, category.path));
            navContainer.addView(item, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(navItemHeightDp())));
            if ("/".equals(category.path)) {
                addRecentNavItem(false);
            }
        }
        if (activeNavButton == null) {
            activeNavButton = search;
        }
        addSettingsNavItem(false);
        addDonationBox();

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        page.addView(content, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));

        TextView header = label(title, 26, TEXT, true);
        header.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

        catalogGrid = new GridView(this);
        catalogGrid.setNumColumns(5);
        catalogGrid.setHorizontalSpacing(dp(18));
        catalogGrid.setVerticalSpacing(dp(catalogVerticalSpacingDp()));
        catalogGrid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        catalogGrid.setSelector(android.R.color.transparent);
        catalogGrid.setFocusable(true);
        catalogGrid.setFocusableInTouchMode(false);
        catalogGrid.setClipToPadding(false);
        catalogGrid.setPadding(0, dp(catalogTopPaddingDp()), 0, dp(catalogBottomPaddingDp()));
        videoAdapter = new VideoAdapter(new ArrayList<>());
        catalogGrid.setAdapter(videoAdapter);
        catalogGrid.setOnItemClickListener((parent, view, position, id) -> {
            VideoItem item = videoAdapter.getItem(position);
            if (item != null) {
                loadDetail(item);
            }
        });
        catalogGrid.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                videoAdapter.setSelectedPosition(position);
                loadMoreCatalogIfNeeded(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                videoAdapter.setSelectedPosition(-1);
            }
        });
        catalogGrid.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (visibleItemCount <= 0 || totalItemCount <= 0) {
                    return;
                }
                loadMoreCatalogIfNeeded(firstVisibleItem + visibleItemCount - 1);
            }
        });
        catalogGrid.setOnTouchListener((view, event) -> {
            if (!PEACH_PATH.equals(currentPath) || catalogLoadingMore) {
                return false;
            }
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                catalogPullTracking = catalogGrid.getFirstVisiblePosition() == 0;
                catalogPullStartY = event.getY();
                return false;
            }
            if (event.getAction() == MotionEvent.ACTION_MOVE && catalogPullTracking) {
                if (event.getY() - catalogPullStartY > dp(120)) {
                    catalogPullTracking = false;
                    refreshPeachCatalogRandomly();
                    return true;
                }
            }
            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                catalogPullTracking = false;
            }
            return false;
        });
        content.addView(catalogGrid, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        addOverlays();
        loadCatalog(title, path);
    }

    private void loadCatalog(String title, String path) {
        catalogPage = 1;
        catalogLoadingMore = false;
        catalogPagingKind = catalogPagingKind(path);
        catalogPagedMode = !catalogPagingKind.isEmpty();
        catalogHasMore = catalogPagedMode;
        setLoading(true, "正在加载" + title + "...");
        executor.execute(() -> {
            try {
                List<VideoItem> videos = siteClient.fetchCatalog(path);
                main.post(() -> {
                    videoAdapter.setItems(videos);
                    setLoading(false, videos.isEmpty() ? "没有解析到影片" : "");
                    if (!videos.isEmpty()) {
                        catalogGrid.requestFocus();
                        catalogGrid.setSelection(0);
                    }
                });
            } catch (Exception e) {
                main.post(() -> setLoading(false, "加载失败：" + e.getMessage()));
            }
        });
    }

    private void loadMoreCatalogIfNeeded(int selectedPosition) {
        if (!catalogPagedMode || catalogLoadingMore || !catalogHasMore || videoAdapter == null) {
            return;
        }
        if (selectedPosition < Math.max(0, videoAdapter.getCount() - 8)) {
            return;
        }
        catalogLoadingMore = true;
        int nextPage = catalogPage + 1;
        String nextPath = pagedCatalogPath(catalogPagingKind, nextPage);
        setLoading(true, "正在加载更多...");
        executor.execute(() -> {
            try {
                List<VideoItem> videos = siteClient.fetchCatalog(nextPath);
                main.post(() -> {
                    catalogLoadingMore = false;
                    setLoading(false, videos.isEmpty() ? "没有更多内容了" : "");
                    if (videos.isEmpty()) {
                        catalogHasMore = false;
                        return;
                    }
                    catalogPage = nextPage;
                    videoAdapter.appendItems(videos);
                });
            } catch (Exception e) {
                main.post(() -> {
                    catalogLoadingMore = false;
                    setLoading(false, "加载更多失败：" + e.getMessage());
                });
            }
        });
    }

    private void refreshPeachCatalogRandomly() {
        if (!PEACH_PATH.equals(currentPath) || videoAdapter == null || catalogLoadingMore) {
            return;
        }
        int page = 1 + random.nextInt(PEACH_RANDOM_PAGE_MAX);
        catalogLoadingMore = true;
        setLoading(true, "正在随机换一批...");
        executor.execute(() -> {
            try {
                List<VideoItem> videos = siteClient.fetchCatalog(pagedCatalogPath("peach", page));
                main.post(() -> {
                    catalogLoadingMore = false;
                    setLoading(false, videos.isEmpty() ? "这一批没有内容" : "");
                    if (videos.isEmpty()) {
                        return;
                    }
                    catalogPage = page;
                    catalogHasMore = true;
                    videoAdapter.setItems(videos);
                    catalogGrid.requestFocus();
                    catalogGrid.setSelection(0);
                });
            } catch (Exception e) {
                main.post(() -> {
                    catalogLoadingMore = false;
                    setLoading(false, "随机刷新失败：" + e.getMessage());
                });
            }
        });
    }

    private void showRecentWatches() {
        screen = Screen.CATALOG;
        currentTitle = "最近观看";
        currentPath = "/";
        currentVideo = null;
        catalogPage = 1;
        catalogLoadingMore = false;
        catalogHasMore = false;
        catalogPagedMode = false;
        catalogPagingKind = "";
        root.removeAllViews();
        root.setBackgroundColor(BG);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.HORIZONTAL);
        page.setPadding(dp(pageHorizontalPaddingDp()), dp(pageTopPaddingDp()), dp(pageHorizontalPaddingDp()), dp(pageBottomPaddingDp()));
        root.addView(page, matchParams());

        LinearLayout left = createLeftRail(page);
        addBrand(left);

        TextView search = searchNavItem(false);
        search.setOnClickListener(v -> showSearch(""));
        navContainer.addView(search, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(navItemHeightDp())));

        activeNavButton = null;
        for (Category category : visibleCategories()) {
            TextView item = navItem(category.name, false);
            item.setOnClickListener(v -> showCatalog(category.name, category.path));
            navContainer.addView(item, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(navItemHeightDp())));
            if ("/".equals(category.path)) {
                activeNavButton = addRecentNavItem(true);
            }
        }
        addSettingsNavItem(false);
        addDonationBox();

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        page.addView(content, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));

        TextView header = label("最近观看", 26, TEXT, true);
        header.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

        catalogGrid = new GridView(this);
        catalogGrid.setNumColumns(5);
        catalogGrid.setHorizontalSpacing(dp(18));
        catalogGrid.setVerticalSpacing(dp(catalogVerticalSpacingDp()));
        catalogGrid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        catalogGrid.setSelector(android.R.color.transparent);
        catalogGrid.setFocusable(true);
        catalogGrid.setFocusableInTouchMode(false);
        catalogGrid.setClipToPadding(false);
        catalogGrid.setPadding(0, dp(catalogTopPaddingDp()), 0, dp(catalogBottomPaddingDp()));
        videoAdapter = new VideoAdapter(loadRecentWatches());
        catalogGrid.setAdapter(videoAdapter);
        catalogGrid.setOnItemClickListener((parent, view, position, id) -> {
            VideoItem item = videoAdapter.getItem(position);
            if (item != null) {
                loadDetail(item);
            }
        });
        catalogGrid.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                videoAdapter.setSelectedPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                videoAdapter.setSelectedPosition(-1);
            }
        });
        content.addView(catalogGrid, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        addOverlays();
        if (videoAdapter.getCount() > 0) {
            catalogGrid.requestFocus();
            catalogGrid.setSelection(0);
        } else {
            showHint("暂无最近观看");
            if (activeNavButton != null) {
                activeNavButton.requestFocus();
            }
        }
    }

    private static String catalogPagingKind(String path) {
        if (MOVIE_TIME_PATH.equals(path)) {
            return "movie";
        }
        if (TV_SHOW_PATH.equals(path)) {
            return "2";
        }
        if (VARIETY_SHOW_PATH.equals(path)) {
            return "3";
        }
        if (ANIMATION_SHOW_PATH.equals(path)) {
            return "4";
        }
        if (PEACH_PATH.equals(path)) {
            return "peach";
        }
        return "";
    }

    private static String pagedCatalogPath(String kind, int page) {
        if ("peach".equals(kind)) {
            return PEACH_PATH + "?page=" + page;
        }
        String id = "movie".equals(kind) ? "1" : kind;
        if (page <= 1) {
            if ("movie".equals(kind)) return MOVIE_TIME_PATH;
            if ("2".equals(kind)) return TV_SHOW_PATH;
            if ("3".equals(kind)) return VARIETY_SHOW_PATH;
            if ("4".equals(kind)) return ANIMATION_SHOW_PATH;
        }
        return "/vod-show/" + id + "--time------" + page + "---.html";
    }

    private void showSearch(String initialKeyword) {
        screen = Screen.SEARCH;
        currentTitle = "搜索";
        currentPath = "/";
        currentVideo = null;
        catalogPage = 1;
        catalogLoadingMore = false;
        catalogHasMore = false;
        catalogPagedMode = false;
        catalogPagingKind = "";
        root.removeAllViews();
        root.setBackgroundColor(BG);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.HORIZONTAL);
        page.setPadding(dp(pageHorizontalPaddingDp()), dp(pageTopPaddingDp()), dp(pageHorizontalPaddingDp()), dp(pageBottomPaddingDp()));
        root.addView(page, matchParams());

        LinearLayout left = createLeftRail(page);
        addBrand(left);

        TextView searchNav = searchNavItem(true);
        activeNavButton = searchNav;
        searchNav.setOnClickListener(v -> showSearch(initialKeyword));
        navContainer.addView(searchNav, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(navItemHeightDp())));
        for (Category category : visibleCategories()) {
            TextView item = navItem(category.name, false);
            item.setOnClickListener(v -> showCatalog(category.name, category.path));
            navContainer.addView(item, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(navItemHeightDp())));
            if ("/".equals(category.path)) {
                addRecentNavItem(false);
            }
        }
        addSettingsNavItem(false);
        addDonationBox();

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        page.addView(content, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));

        LinearLayout searchBar = new LinearLayout(this);
        searchBar.setOrientation(LinearLayout.HORIZONTAL);
        searchBar.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(searchBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(64)));

        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(initialKeyword);
        input.setTextColor(TEXT);
        input.setHintTextColor(MUTED);
        input.setTextSize(20);
        input.setHint("输入片名、演员或关键词");
        input.setFocusable(true);
        input.setBackgroundColor(PANEL);
        input.setPadding(dp(18), 0, dp(18), 0);
        searchBar.addView(input, new LinearLayout.LayoutParams(0, dp(52), 1));

        TextView submit = button("搜索", false);
        LinearLayout.LayoutParams submitParams = new LinearLayout.LayoutParams(dp(120), dp(52));
        submitParams.leftMargin = dp(16);
        searchBar.addView(submit, submitParams);

        catalogGrid = new GridView(this);
        catalogGrid.setNumColumns(5);
        catalogGrid.setHorizontalSpacing(dp(18));
        catalogGrid.setVerticalSpacing(dp(catalogVerticalSpacingDp()));
        catalogGrid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        catalogGrid.setSelector(android.R.color.transparent);
        catalogGrid.setFocusable(true);
        catalogGrid.setClipToPadding(false);
        catalogGrid.setPadding(0, dp(catalogTopPaddingDp()), 0, dp(catalogBottomPaddingDp()));
        videoAdapter = new VideoAdapter(new ArrayList<>());
        catalogGrid.setAdapter(videoAdapter);
        catalogGrid.setOnItemClickListener((parent, view, position, id) -> {
            VideoItem item = videoAdapter.getItem(position);
            if (item != null) {
                loadDetail(item);
            }
        });
        catalogGrid.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                videoAdapter.setSelectedPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                videoAdapter.setSelectedPosition(-1);
            }
        });
        content.addView(catalogGrid, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        View.OnClickListener doSearch = v -> {
            String keyword = input.getText().toString().trim();
            if (keyword.isEmpty()) {
                showHint("请输入搜索关键词");
                input.requestFocus();
            } else {
                performSearch(keyword);
            }
        };
        submit.setOnClickListener(doSearch);
        input.setOnEditorActionListener((v, actionId, event) -> {
            performSearch(input.getText().toString().trim());
            return true;
        });

        addOverlays();
        if (initialKeyword == null || initialKeyword.isEmpty()) {
            setLoading(false, "输入关键词后按搜索");
            input.requestFocus();
        } else {
            performSearch(initialKeyword);
        }
    }

    private void performSearch(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            showHint("请输入搜索关键词");
            return;
        }
        String path = "/vod-search/" + Uri.encode(keyword) + "-------------.html";
        setLoading(true, "正在搜索：" + keyword);
        executor.execute(() -> {
            try {
                List<VideoItem> videos = siteClient.fetchCatalog(path);
                main.post(() -> {
                    videoAdapter.setItems(videos);
                    if (!videos.isEmpty()) {
                        catalogGrid.requestFocus();
                        catalogGrid.setSelection(0);
                    }
                    setLoading(false, videos.isEmpty() ? "没有搜索结果" : "");
                });
            } catch (Exception e) {
                main.post(() -> setLoading(false, "搜索失败：" + e.getMessage()));
            }
        });
    }

    private void showSettings() {
        screen = Screen.SETTINGS;
        currentVideo = null;
        root.removeAllViews();
        root.setBackgroundColor(BG);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.HORIZONTAL);
        page.setPadding(dp(pageHorizontalPaddingDp()), dp(pageTopPaddingDp()), dp(pageHorizontalPaddingDp()), dp(pageBottomPaddingDp()));
        root.addView(page, matchParams());

        LinearLayout left = createLeftRail(page);
        addBrand(left);

        TextView search = searchNavItem(false);
        search.setOnClickListener(v -> showSearch(""));
        navContainer.addView(search, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(navItemHeightDp())));

        for (Category category : visibleCategories()) {
            TextView item = navItem(category.name, false);
            item.setOnClickListener(v -> showCatalog(category.name, category.path));
            navContainer.addView(item, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(navItemHeightDp())));
            if ("/".equals(category.path)) {
                addRecentNavItem(false);
            }
        }
        TextView settingsNav = addSettingsNavItem(true);
        activeNavButton = settingsNav;
        addDonationBox();

        ScrollView contentScroll = new ScrollView(this);
        contentScroll.setFillViewport(false);
        contentScroll.setVerticalScrollBarEnabled(false);
        page.addView(contentScroll, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, 0, 0, dp(24));
        contentScroll.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView header = label("设置", 26, TEXT, true);
        header.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

        TextView preloadInfo = label("", 20, TEXT, true);
        preloadInfo.setGravity(Gravity.CENTER_VERTICAL);
        preloadInfo.setBackgroundColor(PANEL);
        preloadInfo.setPadding(dp(22), 0, dp(22), 0);
        content.addView(preloadInfo, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(64)));

        LinearLayout preloadControls = new LinearLayout(this);
        preloadControls.setOrientation(LinearLayout.HORIZONTAL);
        preloadControls.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(preloadControls, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(62)));

        TextView decreasePreload = button("减少", false);
        TextView increasePreload = button("增加", false);
        LinearLayout.LayoutParams preloadButtonParams = new LinearLayout.LayoutParams(dp(120), dp(48));
        preloadControls.addView(decreasePreload, preloadButtonParams);
        LinearLayout.LayoutParams increaseParams = new LinearLayout.LayoutParams(dp(120), dp(48));
        increaseParams.leftMargin = dp(14);
        preloadControls.addView(increasePreload, increaseParams);

        TextView cacheInfo = label("", 20, TEXT, true);
        cacheInfo.setGravity(Gravity.CENTER_VERTICAL);
        cacheInfo.setBackgroundColor(PANEL);
        cacheInfo.setPadding(dp(22), 0, dp(22), 0);
        content.addView(cacheInfo, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(72)));

        TextView cacheHelp = label("视频缓存用于提前保存 m3u8 切片，最多占用 1GB，超过后会自动清理最旧内容。", 16, MUTED, false);
        cacheHelp.setGravity(Gravity.CENTER_VERTICAL);
        cacheHelp.setPadding(dp(22), 0, dp(22), 0);
        content.addView(cacheHelp, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(72)));

        TextView clear = button("清理视频缓存", false);
        clear.setOnClickListener(v -> clearVideoCache(cacheInfo));
        decreasePreload.setOnClickListener(v -> changePreloadMinutes(-1, preloadInfo));
        increasePreload.setOnClickListener(v -> changePreloadMinutes(1, preloadInfo));
        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(dp(220), dp(54));
        clearParams.topMargin = dp(18);
        content.addView(clear, clearParams);

        TextView clearHistory = button("清除历史浏览记录", false);
        clearHistory.setOnClickListener(v -> clearRecentWatchHistory());
        LinearLayout.LayoutParams clearHistoryParams = new LinearLayout.LayoutParams(dp(260), dp(54));
        clearHistoryParams.topMargin = dp(18);
        content.addView(clearHistory, clearHistoryParams);

        TextView betaMode = button("", false);
        updateBetaModeButton(betaMode);
        betaMode.setOnClickListener(v -> toggleBetaMode(betaMode));
        LinearLayout.LayoutParams betaParams = new LinearLayout.LayoutParams(dp(260), dp(54));
        betaParams.topMargin = dp(18);
        content.addView(betaMode, betaParams);

        TextView versionInfo = label("", 18, MUTED, false);
        versionInfo.setGravity(Gravity.CENTER_VERTICAL);
        versionInfo.setPadding(dp(22), 0, dp(22), 0);
        LinearLayout.LayoutParams versionParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        versionParams.topMargin = dp(16);
        content.addView(versionInfo, versionParams);

        TextView checkUpdate = button("检查更新", false);
        checkUpdate.setOnClickListener(v -> checkForUpdates(false));
        LinearLayout.LayoutParams updateParams = new LinearLayout.LayoutParams(dp(220), dp(54));
        updateParams.topMargin = dp(10);
        content.addView(checkUpdate, updateParams);

        TextView autoUpdate = button("", false);
        updateAutoUpdateButton(autoUpdate);
        autoUpdate.setOnClickListener(v -> toggleAutoUpdateCheck(autoUpdate));
        LinearLayout.LayoutParams autoUpdateParams = new LinearLayout.LayoutParams(dp(300), dp(54));
        autoUpdateParams.topMargin = dp(18);
        content.addView(autoUpdate, autoUpdateParams);

        addOverlays();
        updatePreloadInfo(preloadInfo);
        updateCacheInfo(cacheInfo);
        updateVersionInfo(versionInfo);
        settingsNav.requestFocus();
    }

    private void loadDetail(VideoItem item) {
        currentVideo = item;
        setLoading(true, "正在加载详情...");
        executor.execute(() -> {
            try {
                VideoDetail detail = siteClient.fetchDetail(item);
                main.post(() -> showDetail(detail));
            } catch (Exception e) {
                main.post(() -> setLoading(false, "详情加载失败：" + e.getMessage()));
            }
        });
    }

    private void showDetail(VideoDetail detail) {
        screen = Screen.DETAIL;
        currentDetailEpisodes = detail.episodes;
        activeSourcePosition = 0;
        root.removeAllViews();
        root.setBackgroundColor(BG);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(detailHorizontalPaddingDp()), dp(detailVerticalPaddingDp()), dp(detailHorizontalPaddingDp()), dp(detailVerticalPaddingDp()));
        root.addView(page, matchParams());

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(top, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(detailTopHeightDp())));

        TextView back = button("返回", false);
        back.setOnClickListener(v -> showCatalog(currentTitle, currentPath));
        top.addView(back, new LinearLayout.LayoutParams(dp(detailBackWidthDp()), dp(detailBackHeightDp())));

        TextView title = label(detail.title, detailTitleSp(), TEXT, true);
        title.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, dp(detailTitleHeightDp()), 1);
        titleParams.leftMargin = dp(detailTitleLeftMarginDp());
        top.addView(title, titleParams);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.HORIZONTAL);
        page.addView(body, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        ImageView poster = new ImageView(this);
        poster.setScaleType(ImageView.ScaleType.CENTER_CROP);
        poster.setBackgroundColor(PANEL_2);
        body.addView(poster, new LinearLayout.LayoutParams(dp(detailPosterWidthDp()), dp(detailPosterHeightDp())));
        imageLoader.load(detail.poster, poster);

        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
        rightParams.leftMargin = dp(detailRightMarginDp());
        body.addView(right, rightParams);

        TextView meta = label(detail.meta, detailMetaSp(), MUTED, false);
        meta.setMaxLines(2);
        right.addView(meta, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(detailMetaHeightDp())));

        TextView desc = label(detail.description, detailDescSp(), MUTED, false);
        desc.setLineSpacing(dp(detailDescLineSpacingDp()), 1.0f);
        ScrollView descScroll = new ScrollView(this);
        descScroll.addView(desc);
        right.addView(descScroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(detailDescHeightDp())));

        TextView sourceTitle = label("线路来源", detailSectionTitleSp(), TEXT, true);
        sourceTitle.setGravity(Gravity.CENTER_VERTICAL);
        right.addView(sourceTitle, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(detailSectionTitleHeightDp())));

        currentSources = buildSourceGroups(detail.episodes);
        sourceScroll = new HorizontalScrollView(this);
        sourceScroll.setHorizontalScrollBarEnabled(false);
        sourceScroll.setFocusable(true);
        sourceScroll.setFocusableInTouchMode(false);
        sourceScroll.setOnFocusChangeListener((v, hasFocus) -> refreshSourceRow());
        sourceRow = new LinearLayout(this);
        sourceRow.setOrientation(LinearLayout.HORIZONTAL);
        sourceScroll.addView(sourceRow, new HorizontalScrollView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        populateSourceRow();
        right.addView(sourceScroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(detailSourceRowHeightDp())));

        TextView episodeTitle = label("剧集", detailSectionTitleSp(), TEXT, true);
        episodeTitle.setGravity(Gravity.CENTER_VERTICAL);
        right.addView(episodeTitle, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(detailSectionTitleHeightDp())));

        episodeGrid = new GridView(this);
        episodeGrid.setNumColumns(6);
        episodeGrid.setHorizontalSpacing(dp(detailEpisodeSpacingDp()));
        episodeGrid.setVerticalSpacing(dp(detailEpisodeSpacingDp()));
        episodeGrid.setSelector(android.R.color.transparent);
        episodeGrid.setFocusable(true);
        episodeGrid.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                int position = episodeGrid.getSelectedItemPosition();
                if (position == AdapterView.INVALID_POSITION && episodeAdapter != null && episodeAdapter.getCount() > 0) {
                    position = 0;
                    episodeGrid.setSelection(0);
                }
                if (episodeAdapter != null) {
                    episodeAdapter.forceSelectedPosition(position);
                }
            } else if (episodeAdapter != null) {
                episodeAdapter.forceSelectedPosition(-1);
            }
        });
        episodeAdapter = new EpisodeAdapter(new ArrayList<>());
        episodeGrid.setAdapter(episodeAdapter);
        episodeGrid.setOnItemClickListener((parent, view, position, id) -> playEpisode(episodeAdapter.getItem(position)));
        episodeGrid.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                episodeAdapter.setSelectedPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                episodeAdapter.setSelectedPosition(-1);
            }
        });
        right.addView(episodeGrid, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        addOverlays();
        setLoading(false, detail.episodes.isEmpty() ? "没有解析到剧集" : "");
        if (!detail.episodes.isEmpty()) {
            selectSource(0);
            sourceScroll.requestFocus();
            refreshSourceRow();
        }
    }

    private List<SourceGroup> buildSourceGroups(List<Episode> episodes) {
        LinkedHashMap<Integer, SourceGroup> groups = new LinkedHashMap<>();
        for (Episode episode : episodes) {
            SourceGroup group = groups.get(episode.source);
            if (group == null) {
                group = new SourceGroup(episode.source, episode.sourceLabel());
                groups.put(episode.source, group);
            }
            group.episodes.add(episode);
        }
        ArrayList<SourceGroup> out = new ArrayList<>(groups.values());
        Collections.sort(out, (a, b) -> {
            int priority = Integer.compare(a.priority(), b.priority());
            if (priority != 0) {
                return priority;
            }
            return Integer.compare(a.source, b.source);
        });
        for (SourceGroup group : out) {
            Collections.sort(group.episodes, (a, b) -> Integer.compare(a.index, b.index));
        }
        return out;
    }

    private void selectSource(int position) {
        if (episodeAdapter == null || currentSources.isEmpty()) {
            return;
        }
        int safePosition = Math.max(0, Math.min(position, currentSources.size() - 1));
        activeSourcePosition = safePosition;
        refreshSourceRow();
        SourceGroup source = currentSources.get(safePosition);
        episodeAdapter.setEpisodes(source == null ? new ArrayList<>() : source.episodes);
        if (episodeGrid == null || !episodeGrid.hasFocus()) {
            episodeAdapter.forceSelectedPosition(-1);
        }
        if (episodeGrid != null && episodeAdapter.getCount() > 0) {
            episodeGrid.setSelection(0);
        }
    }

    private void populateSourceRow() {
        if (sourceRow == null) {
            return;
        }
        sourceRow.removeAllViews();
        for (int i = 0; i < currentSources.size(); i++) {
            final int index = i;
            TextView view = label(currentSources.get(i).title, detailSourceTextSp(), TEXT, true);
            view.setGravity(Gravity.CENTER);
            view.setSingleLine(true);
            view.setFocusable(false);
            view.setPadding(dp(detailSourceHorizontalPaddingDp()), 0, dp(detailSourceHorizontalPaddingDp()), 0);
            view.setOnClickListener(v -> selectSource(index));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(detailSourceButtonWidthDp()), dp(detailSourceButtonHeightDp()));
            params.rightMargin = dp(detailSourceButtonGapDp());
            sourceRow.addView(view, params);
        }
        refreshSourceRow();
    }

    private void refreshSourceRow() {
        if (sourceRow == null) {
            return;
        }
        boolean focused = sourceScroll != null && sourceScroll.hasFocus();
        for (int i = 0; i < sourceRow.getChildCount(); i++) {
            View child = sourceRow.getChildAt(i);
            if (!(child instanceof TextView)) {
                continue;
            }
            TextView view = (TextView) child;
            boolean active = i == activeSourcePosition;
            boolean highlighted = active && focused;
            view.setTextColor(highlighted ? Color.BLACK : active ? ACCENT : TEXT);
            view.setBackgroundColor(highlighted ? ACCENT : PANEL);
        }
        scrollActiveSourceIntoView();
    }

    private void scrollActiveSourceIntoView() {
        if (sourceScroll == null || sourceRow == null || activeSourcePosition < 0 || activeSourcePosition >= sourceRow.getChildCount()) {
            return;
        }
        View active = sourceRow.getChildAt(activeSourcePosition);
        sourceScroll.post(() -> sourceScroll.smoothScrollTo(Math.max(0, active.getLeft() - dp(16)), 0));
    }

    private void playEpisode(Episode episode) {
        if (episode == null) {
            return;
        }
        pendingEpisode = episode;
        List<Episode> candidates = playbackCandidates(episode);
        setLoading(true, "正在解析播放地址...");
        executor.execute(() -> {
            PlayTarget fallback = null;
            Episode fallbackEpisode = episode;
            for (Episode candidate : candidates) {
                try {
                    PlayTarget target = siteClient.resolvePlayTarget(candidate);
                    if (target.isDirectVideo()) {
                        Episode resolvedEpisode = candidate;
                        main.post(() -> {
                            pendingEpisode = resolvedEpisode;
                            setLoading(false, "");
                            showVideoPlayer(target);
                        });
                        return;
                    }
                    if (fallback == null || candidate.sourcePriority() < fallbackEpisode.sourcePriority()) {
                        fallback = target;
                        fallbackEpisode = candidate;
                    }
                } catch (Exception ignored) {
                }
            }
            PlayTarget target = fallback != null ? fallback : new PlayTarget(episode.title, absolutize(episode.path), "", episode.from);
            Episode resolvedEpisode = fallbackEpisode;
            main.post(() -> {
                pendingEpisode = resolvedEpisode;
                setLoading(false, "正在尝试捕获 m3u8");
                captureM3u8WithWebView(target);
            });
        });
    }
    private List<Episode> playbackCandidates(Episode episode) {
        ArrayList<Episode> candidates = new ArrayList<>();
        if (episodeAdapter != null) {
            for (int i = 0; i < episodeAdapter.getCount(); i++) {
                Episode candidate = episodeAdapter.getItem(i);
                if (candidate != null && candidate.index == episode.index && !candidates.contains(candidate)) {
                    candidates.add(candidate);
                }
            }
        }
        if (!candidates.contains(episode)) {
            candidates.add(episode);
        }
        for (Episode candidate : currentDetailEpisodes) {
            if (candidate != null && candidate.index == episode.index && !candidates.contains(candidate)) {
                candidates.add(candidate);
            }
        }
        Collections.sort(candidates, (a, b) -> {
            int selected = selectedCandidateCompare(a, b, episode);
            if (selected != 0) {
                return selected;
            }
            int priority = Integer.compare(a.sourcePriority(), b.sourcePriority());
            if (priority != 0) {
                return priority;
            }
            return Integer.compare(a.source, b.source);
        });
        return candidates;
    }

    private static int selectedCandidateCompare(Episode a, Episode b, Episode selected) {
        boolean aSelected = a == selected;
        boolean bSelected = b == selected;
        if (aSelected == bSelected || selected.sourcePriority() > 1) {
            return 0;
        }
        return aSelected ? -1 : 1;
    }

    private void captureM3u8WithWebView(PlayTarget target) {
        pendingTarget = target;
        resolverFinished = false;
        setLoading(true, "正在捕获 m3u8...");
        if (resolverWebView != null) {
            resolverWebView.destroy();
        }
        resolverWebView = new WebView(this);
        resolverWebView.setVisibility(View.GONE);
        WebSettings settings = resolverWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setUserAgentString(settings.getUserAgentString() + " YfVodTVCapture/1.0 AndroidTV");
        resolverWebView.addJavascriptInterface(new M3u8Bridge(), "YfVodCapture");
        resolverWebView.setWebViewClient(new CaptureClient());
        root.addView(resolverWebView, new FrameLayout.LayoutParams(1, 1));
        resolverWebView.loadUrl(target.webUrl);

        main.postDelayed(() -> {
            if (!resolverFinished && pendingTarget == target) {
                resolverFinished = true;
                cleanupResolver();
                setLoading(false, "未捕获到 m3u8，请换“国际/亚太”等 m3u8 线路");
                screen = Screen.DETAIL;
                if (episodeGrid != null) {
                    episodeGrid.requestFocus();
                    if (pendingEpisode != null && episodeAdapter != null) {
                        int index = episodeAdapter.indexOf(pendingEpisode);
                        if (index >= 0) {
                            episodeGrid.setSelection(index);
                        }
                    }
                }
            }
        }, 9000);
    }

    private void onM3u8Captured(String url, String source) {
        if (url == null || url.isEmpty() || resolverFinished || pendingTarget == null) {
            return;
        }
        resolverFinished = true;
        PlayTarget target = new PlayTarget(pendingTarget.title, pendingTarget.webUrl, url, pendingTarget.from);
        cleanupResolver();
        setLoading(false, "");
        showHint("已捕获 m3u8：" + source);
        showVideoPlayer(target);
    }

    private void cleanupResolver() {
        if (resolverWebView != null) {
            if (resolverWebView.getParent() instanceof ViewGroup) {
                ((ViewGroup) resolverWebView.getParent()).removeView(resolverWebView);
            }
            resolverWebView.destroy();
            resolverWebView = null;
        }
    }

    private void showWebPlayer(PlayTarget target) {
        screen = Screen.PLAYER;
        root.removeAllViews();
        root.setBackgroundColor(Color.BLACK);

        playerWebView = new WebView(this);
        playerWebView.setBackgroundColor(Color.BLACK);
        playerWebView.setFocusable(true);
        playerWebView.setFocusableInTouchMode(true);
        WebSettings settings = playerWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setUserAgentString(settings.getUserAgentString() + " YfVodTVNative/1.0 AndroidTV");
        playerWebView.setWebViewClient(new CaptureClient());
        playerWebView.addJavascriptInterface(new M3u8Bridge(), "YfVodCapture");
        playerWebView.setWebChromeClient(new PlayerChromeClient());
        root.addView(playerWebView, matchParams());
        addOverlays();
        showHint("网页播放器：" + target.title);
        playerWebView.loadUrl(target.webUrl);
        playerWebView.requestFocus();
    }

    private void showVideoPlayer(PlayTarget target) {
        try {
            screen = Screen.PLAYER;
            root.removeAllViews();
            root.setBackgroundColor(Color.BLACK);
            playerView = new PlayerView(this);
            playerView.setUseController(true);
            playerView.setControllerAutoShow(true);
            playerView.setKeepScreenOn(true);
            playerView.setShowFastForwardButton(true);
            playerView.setShowRewindButton(true);
            exoPlayer = new ExoPlayer.Builder(this)
                    .setLoadControl(createLoadControl())
                    .setMediaSourceFactory(createMediaSourceFactory())
                    .build();
            exoPlayer.addListener(new Player.Listener() {
                @Override
                public void onPlayerError(PlaybackException error) {
                    showHint("原生播放失败，请换一条 m3u8 线路");
                    releaseNativePlayer();
                    closePlayer();
                }
            });
            playerView.setPlayer(exoPlayer);
            root.addView(playerView, matchParams());
            addOverlays();
            saveRecentWatch();
            showHint("正在播放：" + target.title);
            exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(target.directUrl)));
            exoPlayer.prepare();
            exoPlayer.play();
            playerView.requestFocus();
        } catch (Exception e) {
            showHint("播放器启动失败，请换一条线路");
            releaseNativePlayer();
            closePlayer();
        }
    }

    private void releaseNativePlayer() {
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
        if (playerView != null) {
            playerView.setPlayer(null);
            playerView = null;
        }
    }

    private DefaultLoadControl createLoadControl() {
        int preloadMs = getPreloadMinutes() * 60_000;
        return new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        30_000,
                        preloadMs,
                        2_500,
                        5_000
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build();
    }

    private DefaultMediaSourceFactory createMediaSourceFactory() {
        DefaultHttpDataSource.Factory httpFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Linux; Android TV) AppleWebKit/537.36 ZiwenPlayer/1.0")
                .setConnectTimeoutMs(10_000)
                .setReadTimeoutMs(20_000)
                .setAllowCrossProtocolRedirects(true);
        DefaultDataSource.Factory upstreamFactory = new DefaultDataSource.Factory(this, httpFactory);
        CacheDataSource.Factory cacheDataSourceFactory = new CacheDataSource.Factory()
                .setCache(getVideoCache())
                .setUpstreamDataSourceFactory(upstreamFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
        return new DefaultMediaSourceFactory(this)
                .setDataSourceFactory(cacheDataSourceFactory);
    }

    private synchronized SimpleCache getVideoCache() {
        if (videoCache == null) {
            if (cacheDatabaseProvider == null) {
                cacheDatabaseProvider = new StandaloneDatabaseProvider(this);
            }
            videoCache = new SimpleCache(
                    videoCacheDir(),
                    new LeastRecentlyUsedCacheEvictor(VIDEO_CACHE_MAX_BYTES),
                    cacheDatabaseProvider
            );
        }
        return videoCache;
    }

    private File videoCacheDir() {
        return new File(getCacheDir(), "video_cache");
    }

    private SharedPreferences settingsPrefs() {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }

    private boolean isBetaModeEnabled() {
        return settingsPrefs().getBoolean(PREF_BETA_MODE, true);
    }

    private List<Category> visibleCategories() {
        ArrayList<Category> items = new ArrayList<>();
        boolean betaMode = isBetaModeEnabled();
        for (Category category : categories) {
            if (PEACH_PATH.equals(category.path) && !betaMode) {
                continue;
            }
            items.add(category);
        }
        return items;
    }

    private void toggleBetaMode(TextView target) {
        boolean enabled = !isBetaModeEnabled();
        settingsPrefs().edit().putBoolean(PREF_BETA_MODE, enabled).apply();
        updateBetaModeButton(target);
        showHint(enabled ? "内测模式已打开" : "内测模式已关闭");
        showSettings();
    }

    private void updateBetaModeButton(TextView target) {
        if (target != null) {
            target.setText(isBetaModeEnabled() ? "内测模式：已开启" : "内测模式：未开启");
        }
    }

    private boolean isAutoUpdateCheckEnabled() {
        return settingsPrefs().getBoolean(PREF_AUTO_UPDATE_CHECK, false);
    }

    private void toggleAutoUpdateCheck(TextView target) {
        boolean enabled = !isAutoUpdateCheckEnabled();
        settingsPrefs().edit().putBoolean(PREF_AUTO_UPDATE_CHECK, enabled).apply();
        updateAutoUpdateButton(target);
        showHint(enabled ? "启动时自动检查更新已开启" : "启动时自动检查更新已关闭");
    }

    private void updateAutoUpdateButton(TextView target) {
        if (target != null) {
            target.setText(isAutoUpdateCheckEnabled() ? "自动检查更新：已开启" : "自动检查更新：未开启");
        }
    }

    private void updateVersionInfo(TextView target) {
        if (target != null) {
            target.setText("当前版本：" + currentVersionName());
        }
    }

    private void maybeCheckUpdateOnStartup() {
        if (!isAutoUpdateCheckEnabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        long last = settingsPrefs().getLong(PREF_LAST_AUTO_UPDATE_CHECK, 0L);
        if (now - last < AUTO_UPDATE_CHECK_INTERVAL_MS) {
            return;
        }
        settingsPrefs().edit().putLong(PREF_LAST_AUTO_UPDATE_CHECK, now).apply();
        main.postDelayed(() -> checkForUpdates(true), 1800);
    }

    private void checkForUpdates(boolean automatic) {
        if (updateChecking || updateDownloading) {
            if (!automatic) {
                showHint("正在检查或下载更新");
            }
            return;
        }
        updateChecking = true;
        if (!automatic) {
            setLoading(true, "正在检查更新...");
        }
        executor.execute(() -> {
            try {
                UpdateInfo info = fetchLatestUpdate();
                main.post(() -> {
                    updateChecking = false;
                    setLoading(false, "");
                    if (info == null || info.apkUrl.isEmpty()) {
                        if (!automatic) {
                            showHint("没有找到可安装的 APK");
                        }
                        return;
                    }
                    if (compareVersionNames(info.versionName, currentVersionName()) <= 0) {
                        if (!automatic) {
                            showHint("已是最新版本：" + currentVersionName());
                        }
                        return;
                    }
                    showUpdatePrompt(info);
                });
            } catch (Exception e) {
                main.post(() -> {
                    updateChecking = false;
                    setLoading(false, "");
                    if (!automatic) {
                        showHint("检查更新失败：" + e.getMessage());
                    }
                });
            }
        });
    }

    private void showUpdatePrompt(UpdateInfo info) {
        if (info == null || info.apkUrl.isEmpty() || isFinishing()) {
            return;
        }
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("发现新版本")
                .setMessage("当前版本：" + currentVersionName()
                        + "\n最新版本：" + info.versionName
                        + "\n\n是否立即下载并安装更新？")
                .setPositiveButton("立即更新", (ignored, which) -> downloadUpdate(info))
                .setNegativeButton("稍后", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            TextView updateButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            TextView laterButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            updateButton.setTextColor(ACCENT);
            laterButton.setTextColor(TEXT);
            updateButton.requestFocus();
        });
        dialog.setOnDismissListener(ignored -> keepScreenImmersive());
        dialog.show();
    }

    private UpdateInfo fetchLatestUpdate() throws Exception {
        String raw = readUpdateTextWithRetry(UPDATE_API_URL);
        JSONObject release = new JSONObject(raw);
        String tag = release.optString("tag_name", "");
        String versionName = normalizeVersionName(tag);
        int versionCode = versionCodeFromName(versionName);
        String apkUrl = "";
        String apkName = "";
        JSONArray assets = release.optJSONArray("assets");
        if (assets != null) {
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.optJSONObject(i);
                if (asset == null) {
                    continue;
                }
                String name = asset.optString("name", "");
                if (name.startsWith(UPDATE_APK_PREFIX) && name.endsWith(UPDATE_APK_SUFFIX)) {
                    apkName = name;
                    apkUrl = asset.optString("browser_download_url", "");
                    break;
                }
            }
        }
        return new UpdateInfo(versionName, versionCode, apkName, apkUrl);
    }

    private static String readUpdateTextWithRetry(String url) throws Exception {
        Exception last = null;
        for (int i = 0; i < 3; i++) {
            try {
                return readUpdateText(url);
            } catch (Exception e) {
                last = e;
            }
        }
        throw last == null ? new IllegalStateException("检查更新失败") : last;
    }

    private void downloadUpdate(UpdateInfo info) {
        if (info == null || info.apkUrl.isEmpty()) {
            showHint("更新地址无效");
            return;
        }
        try {
            updateDownloading = true;
            updateDownloadVersion = info.versionName;
            String fileName = info.apkName == null || info.apkName.isEmpty() ? "Ziwen-Player-v" + info.versionName + ".apk" : info.apkName;
            updateDownloadFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName);
            File parent = updateDownloadFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            if (updateDownloadFile.exists()) {
                updateDownloadFile.delete();
            }
            setLoading(true, "正在下载更新...");
            executor.execute(() -> {
                try {
                    downloadUpdateFileWithRetry(info.apkUrl, updateDownloadFile);
                    main.post(() -> {
                        updateDownloading = false;
                        setLoading(false, "");
                        installDownloadedApk();
                    });
                } catch (Exception e) {
                    main.post(() -> {
                        updateDownloading = false;
                        setLoading(false, "");
                        showHint("下载更新失败：" + e.getMessage());
                    });
                }
            });
        } catch (Exception e) {
            updateDownloading = false;
            setLoading(false, "");
            showHint("下载更新失败：" + e.getMessage());
        }
    }

    private static void downloadUpdateFileWithRetry(String url, File target) throws Exception {
        Exception last = null;
        for (int i = 0; i < 3; i++) {
            try {
                downloadUpdateFile(url, target);
                return;
            } catch (Exception e) {
                last = e;
                if (target != null && target.exists()) {
                    target.delete();
                }
            }
        }
        throw last == null ? new IllegalStateException("下载失败") : last;
    }

    private static void downloadUpdateFile(String url, File target) throws Exception {
        HttpURLConnection connection = openUpdateConnection(url);
        connection.setInstanceFollowRedirects(true);
        int code = connection.getResponseCode();
        if (code >= 300 && code < 400) {
            String location = connection.getHeaderField("Location");
            connection.disconnect();
            if (location == null || location.isEmpty()) {
                throw new IllegalStateException("下载重定向无效");
            }
            downloadUpdateFile(location, target);
            return;
        }
        if (code < 200 || code >= 300) {
            throw new IllegalStateException("HTTP " + code);
        }
        try (InputStream input = connection.getInputStream();
             FileOutputStream output = new FileOutputStream(target)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        } finally {
            connection.disconnect();
        }
        if (!target.exists() || target.length() < 1024 * 1024) {
            throw new IllegalStateException("安装包不完整");
        }
    }

    private static String readUpdateText(String url) throws Exception {
        HttpURLConnection connection = openUpdateConnection(url);
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
        } finally {
            connection.disconnect();
        }
        return builder.toString();
    }

    private static HttpURLConnection openUpdateConnection(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(20_000);
        connection.setReadTimeout(60_000);
        connection.setRequestProperty("User-Agent", "Ziwen-Player-Updater/1.0 Android");
        connection.setRequestProperty("Accept", "application/vnd.github+json,application/octet-stream,*/*");
        return connection;
    }

    private void installDownloadedApk() {
        if (updateDownloadFile == null || !updateDownloadFile.exists()) {
            showHint("安装包路径无效");
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !getPackageManager().canRequestPackageInstalls()) {
            showHint("请允许安装未知来源应用后再安装");
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            return;
        }
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", updateDownloadFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            showHint("下载完成，请确认安装 " + updateDownloadVersion);
            startActivity(intent);
        } catch (Exception e) {
            showHint("打开安装器失败：" + e.getMessage());
        }
    }

    private void resumePendingUpdateInstallIfAllowed() {
        if (updateDownloadFile == null || !updateDownloadFile.exists()) {
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || getPackageManager().canRequestPackageInstalls()) {
            main.postDelayed(this::installDownloadedApk, 600);
        }
    }

    private String currentVersionName() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            return info.versionName == null ? "" : info.versionName;
        } catch (Exception ignored) {
            return "";
        }
    }

    private long currentVersionCode() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return info.getLongVersionCode();
            }
            return info.versionCode;
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static String normalizeVersionName(String tag) {
        if (tag == null) {
            return "";
        }
        String value = tag.trim();
        if (value.startsWith("v") || value.startsWith("V")) {
            value = value.substring(1);
        }
        return value;
    }

    private static int versionCodeFromName(String versionName) {
        if (versionName == null || versionName.isEmpty()) {
            return 0;
        }
        Matcher matcher = Pattern.compile("(\\d+)").matcher(versionName);
        int code = 0;
        while (matcher.find()) {
            code = code * 100 + safeParseInt(matcher.group(1));
        }
        return code;
    }

    private static int compareVersionNames(String left, String right) {
        int[] a = versionParts(left);
        int[] b = versionParts(right);
        int count = Math.max(a.length, b.length);
        for (int i = 0; i < count; i++) {
            int av = i < a.length ? a[i] : 0;
            int bv = i < b.length ? b[i] : 0;
            if (av != bv) {
                return av > bv ? 1 : -1;
            }
        }
        return 0;
    }

    private static int[] versionParts(String versionName) {
        ArrayList<Integer> parts = new ArrayList<>();
        Matcher matcher = Pattern.compile("(\\d+)").matcher(versionName == null ? "" : versionName);
        while (matcher.find()) {
            parts.add(safeParseInt(matcher.group(1)));
        }
        int[] out = new int[parts.size()];
        for (int i = 0; i < parts.size(); i++) {
            out[i] = parts.get(i);
        }
        return out;
    }

    private static int safeParseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private List<VideoItem> loadRecentWatches() {
        ArrayList<VideoItem> items = new ArrayList<>();
        String raw = settingsPrefs().getString(PREF_RECENT_WATCHES, "");
        if (raw == null || raw.isEmpty()) {
            return items;
        }
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length() && items.size() < RECENT_WATCH_LIMIT; i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) {
                    continue;
                }
                String title = object.optString("title", "");
                String url = object.optString("url", "");
                String poster = object.optString("poster", "");
                String remarks = object.optString("remarks", "");
                String provider = object.optString("provider", url.startsWith(PEACH_PATH) ? "peach" : "");
                String remoteId = object.optString("remoteId", url.startsWith(PEACH_PATH + "/detail/") ? url.substring((PEACH_PATH + "/detail/").length()) : "");
                String playUrl = object.optString("playUrl", "");
                if (url.startsWith(PEACH_PATH) && !isBetaModeEnabled()) {
                    continue;
                }
                if (!title.isEmpty() && !url.isEmpty()) {
                    items.add(new VideoItem(title, url, poster, remarks, provider, remoteId, playUrl));
                }
            }
        } catch (Exception ignored) {
            settingsPrefs().edit().remove(PREF_RECENT_WATCHES).apply();
        }
        settingsPrefs().edit().remove(PREF_RECENT_WATCHES_LEGACY).apply();
        return items;
    }

    private void saveRecentWatch() {
        if (currentVideo == null || currentVideo.url == null || currentVideo.url.isEmpty()) {
            return;
        }
        ArrayList<VideoItem> items = new ArrayList<>();
        items.add(currentVideo);
        for (VideoItem item : loadRecentWatches()) {
            if (item == null || item.url == null || item.url.equals(currentVideo.url)) {
                continue;
            }
            items.add(item);
            if (items.size() >= RECENT_WATCH_LIMIT) {
                break;
            }
        }
        JSONArray array = new JSONArray();
        try {
            for (VideoItem item : items) {
                JSONObject object = new JSONObject();
                object.put("title", nullToEmpty(item.title));
                object.put("url", nullToEmpty(item.url));
                object.put("poster", nullToEmpty(item.poster));
                object.put("remarks", nullToEmpty(item.remarks));
                object.put("provider", nullToEmpty(item.provider));
                object.put("remoteId", nullToEmpty(item.remoteId));
                object.put("playUrl", nullToEmpty(item.playUrl));
                array.put(object);
            }
            settingsPrefs().edit().putString(PREF_RECENT_WATCHES, array.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    private void clearRecentWatchHistory() {
        settingsPrefs().edit()
                .remove(PREF_RECENT_WATCHES)
                .remove(PREF_RECENT_WATCHES_LEGACY)
                .apply();
        showHint("历史浏览记录已清除");
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private int getPreloadMinutes() {
        int value = settingsPrefs().getInt(PREF_PRELOAD_MINUTES, DEFAULT_PRELOAD_MINUTES);
        for (int option : PRELOAD_MINUTE_OPTIONS) {
            if (option == value) {
                return value;
            }
        }
        return DEFAULT_PRELOAD_MINUTES;
    }

    private void changePreloadMinutes(int direction, TextView target) {
        int current = getPreloadMinutes();
        int index = 0;
        for (int i = 0; i < PRELOAD_MINUTE_OPTIONS.length; i++) {
            if (PRELOAD_MINUTE_OPTIONS[i] == current) {
                index = i;
                break;
            }
        }
        int nextIndex = Math.max(0, Math.min(PRELOAD_MINUTE_OPTIONS.length - 1, index + direction));
        int next = PRELOAD_MINUTE_OPTIONS[nextIndex];
        settingsPrefs().edit().putInt(PREF_PRELOAD_MINUTES, next).apply();
        updatePreloadInfo(target);
        showHint("提前缓冲已设为 " + next + " 分钟");
    }

    private void updatePreloadInfo(TextView target) {
        if (target != null) {
            target.setText("提前缓冲：" + getPreloadMinutes() + " 分钟");
        }
    }

    private void updateCacheInfo(TextView target) {
        if (target == null) {
            return;
        }
        executor.execute(() -> {
            long bytes = folderSize(videoCacheDir());
            main.post(() -> target.setText("视频缓存：" + formatBytes(bytes) + " / " + formatBytes(VIDEO_CACHE_MAX_BYTES)));
        });
    }

    private void clearVideoCache(TextView cacheInfo) {
        setLoading(true, "正在清理缓存...");
        executor.execute(() -> {
            releaseVideoCache();
            deleteRecursively(videoCacheDir());
            main.post(() -> {
                setLoading(false, "缓存已清理");
                updateCacheInfo(cacheInfo);
            });
        });
    }

    private void clearVideoCacheOnStartup() {
        executor.execute(() -> {
            releaseVideoCache();
            deleteRecursively(videoCacheDir());
        });
    }

    private synchronized void releaseVideoCache() {
        if (videoCache != null) {
            videoCache.release();
            videoCache = null;
        }
    }

    private static long folderSize(File file) {
        if (file == null || !file.exists()) {
            return 0L;
        }
        if (file.isFile()) {
            return file.length();
        }
        long size = 0L;
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                size += folderSize(child);
            }
        }
        return size;
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }

    private static String formatBytes(long bytes) {
        if (bytes >= 1024L * 1024L * 1024L) {
            return String.format(Locale.ROOT, "%.2fGB", bytes / 1024f / 1024f / 1024f);
        }
        if (bytes >= 1024L * 1024L) {
            return String.format(Locale.ROOT, "%.1fMB", bytes / 1024f / 1024f);
        }
        if (bytes >= 1024L) {
            return String.format(Locale.ROOT, "%.1fKB", bytes / 1024f);
        }
        return bytes + "B";
    }

    private void createLoading() {
        loading = new ProgressBar(this, null, android.R.attr.progressBarStyleLarge);
        loading.setVisibility(View.GONE);
    }

    private void createToast() {
        toast = label("", 18, TEXT, false);
        toast.setGravity(Gravity.CENTER);
        toast.setBackgroundColor(0xDD000000);
        toast.setVisibility(View.GONE);
    }

    private void addOverlays() {
        if (loading.getParent() != null) {
            ((ViewGroup) loading.getParent()).removeView(loading);
        }
        FrameLayout.LayoutParams loadingParams = new FrameLayout.LayoutParams(dp(72), dp(72), Gravity.CENTER);
        root.addView(loading, loadingParams);

        if (toast.getParent() != null) {
            ((ViewGroup) toast.getParent()).removeView(toast);
        }
        FrameLayout.LayoutParams toastParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(52), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        toastParams.bottomMargin = dp(24);
        root.addView(toast, toastParams);
    }

    private void setLoading(boolean show, String message) {
        loading.setVisibility(show ? View.VISIBLE : View.GONE);
        if (message != null && !message.isEmpty()) {
            showHint(message);
        }
    }

    private void showHint(String message) {
        toast.setText("  " + message + "  ");
        toast.setVisibility(View.VISIBLE);
        toast.removeCallbacks(null);
        toast.postDelayed(() -> toast.setVisibility(View.GONE), 2200);
    }

    private LinearLayout createLeftRail(LinearLayout page) {
        LinearLayout left = new LinearLayout(this);
        navRail = left;
        left.setOrientation(LinearLayout.VERTICAL);
        left.setPadding(0, 0, dp(navEndPaddingDp()), 0);
        page.addView(left, new LinearLayout.LayoutParams(dp(navWidthDp()), ViewGroup.LayoutParams.MATCH_PARENT));
        return left;
    }

    private void addBrand(LinearLayout left) {
        TextView brand = label("子文播放器", brandTextSp(), TEXT, true);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        left.addView(brand, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(brandHeightDp())));

        navContainer = new LinearLayout(this);
        navContainer.setOrientation(LinearLayout.VERTICAL);
        ScrollView navScroll = new ScrollView(this);
        navScroll.setFillViewport(false);
        navScroll.setVerticalScrollBarEnabled(false);
        navScroll.addView(navContainer, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        left.addView(navScroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
    }

    private TextView navItem(String text, boolean selected) {
        TextView view = label(text, navTextSp(), selected ? ACCENT : TEXT, true);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(20), 0, dp(12), 0);
        view.setFocusable(true);
        view.setBackgroundColor(PANEL);
        view.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                view.setTextColor(Color.BLACK);
                view.setBackgroundColor(ACCENT);
            } else {
                view.setTextColor(selected ? ACCENT : TEXT);
                view.setBackgroundColor(PANEL);
            }
        });
        return view;
    }

    private TextView searchNavItem(boolean selected) {
        TextView view = label("⌕  搜索", 18, selected ? ACCENT : TEXT, true);
        view.setGravity(Gravity.CENTER);
        view.setFocusable(true);
        view.setBackgroundColor(PANEL);
        view.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                view.setTextColor(Color.BLACK);
                view.setBackgroundColor(ACCENT);
            } else {
                view.setTextColor(selected ? ACCENT : TEXT);
                view.setBackgroundColor(PANEL);
            }
        });
        return view;
    }

    private TextView addSettingsNavItem(boolean selected) {
        TextView item = navItem("设置", selected);
        item.setOnClickListener(v -> showSettings());
        navContainer.addView(item, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(navItemHeightDp())));
        return item;
    }

    private TextView addRecentNavItem(boolean selected) {
        TextView item = navItem("最近观看", selected);
        item.setOnClickListener(v -> showRecentWatches());
        navContainer.addView(item, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(navItemHeightDp())));
        return item;
    }

    private void addDonationBox() {
        if (navRail == null) {
            return;
        }
        TextView tip = label("如果喜欢请支付宝扫一扫赞助我", donationTipTextSp(), MUTED, false);
        tip.setGravity(Gravity.CENTER);
        tip.setMaxLines(2);
        navRail.addView(tip, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(donationTipHeightDp())));

        ImageView qr = new ImageView(this);
        qr.setImageResource(getResources().getIdentifier("donation_qr", "drawable", getPackageName()));
        qr.setScaleType(ImageView.ScaleType.FIT_CENTER);
        qr.setFocusable(false);
        LinearLayout.LayoutParams qrParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(donationQrHeightDp()));
        qrParams.topMargin = dp(donationQrTopMarginDp());
        navRail.addView(qr, qrParams);
    }

    private TextView button(String text, boolean selected) {
        TextView view = label(text, 18, selected ? Color.BLACK : TEXT, true);
        view.setGravity(Gravity.CENTER);
        view.setFocusable(true);
        view.setBackgroundColor(selected ? ACCENT : PANEL);
        view.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                view.setTextColor(Color.BLACK);
                view.setBackgroundColor(ACCENT);
            } else {
                view.setTextColor(selected ? Color.BLACK : TEXT);
                view.setBackgroundColor(selected ? ACCENT : PANEL);
            }
        });
        return view;
    }

    private TextView label(String text, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text == null ? "" : text);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setIncludeFontPadding(true);
        if (bold) {
            view.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        }
        return view;
    }

    private static String absolutize(String path) {
        if (path == null || path.isEmpty()) {
            return BASE_URL + "/";
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return BASE_URL + path;
    }

    private FrameLayout.LayoutParams matchParams() {
        return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private int screenHeightDp() {
        return (int) (getResources().getDisplayMetrics().heightPixels / getResources().getDisplayMetrics().density);
    }

    private boolean tinyLandscape() {
        return screenHeightDp() < 420;
    }

    private boolean compactLandscape() {
        return screenHeightDp() < 540;
    }

    private int pageHorizontalPaddingDp() {
        return tinyLandscape() ? 16 : (compactLandscape() ? 22 : 32);
    }

    private int pageTopPaddingDp() {
        return tinyLandscape() ? 8 : (compactLandscape() ? 12 : 24);
    }

    private int pageBottomPaddingDp() {
        return tinyLandscape() ? 8 : (compactLandscape() ? 10 : 18);
    }

    private int navWidthDp() {
        return tinyLandscape() ? 138 : (compactLandscape() ? 152 : 170);
    }

    private int navEndPaddingDp() {
        return tinyLandscape() ? 12 : (compactLandscape() ? 16 : 24);
    }

    private int brandHeightDp() {
        return tinyLandscape() ? 36 : (compactLandscape() ? 46 : 64);
    }

    private int brandTextSp() {
        return tinyLandscape() ? 20 : (compactLandscape() ? 22 : 26);
    }

    private int navItemHeightDp() {
        return tinyLandscape() ? 34 : (compactLandscape() ? 42 : 54);
    }

    private int navTextSp() {
        return tinyLandscape() ? 15 : (compactLandscape() ? 16 : 18);
    }

    private int donationTipTextSp() {
        return tinyLandscape() ? 9 : (compactLandscape() ? 10 : 12);
    }

    private int donationTipHeightDp() {
        return tinyLandscape() ? 28 : (compactLandscape() ? 32 : 40);
    }

    private int donationQrHeightDp() {
        return tinyLandscape() ? 58 : (compactLandscape() ? 82 : 118);
    }

    private int donationQrTopMarginDp() {
        return tinyLandscape() ? 2 : (compactLandscape() ? 4 : 6);
    }

    private int catalogVerticalSpacingDp() {
        return tinyLandscape() ? 8 : (compactLandscape() ? 10 : 14);
    }

    private int catalogTopPaddingDp() {
        return tinyLandscape() ? 2 : (compactLandscape() ? 4 : 6);
    }

    private int catalogBottomPaddingDp() {
        return tinyLandscape() ? 2 : (compactLandscape() ? 4 : 8);
    }

    private int cardPaddingDp() {
        return tinyLandscape() ? 4 : (compactLandscape() ? 5 : 7);
    }

    private int videoPosterHeightDp() {
        return tinyLandscape() ? 128 : (compactLandscape() ? 154 : 190);
    }

    private int videoTitleSp() {
        return tinyLandscape() ? 13 : (compactLandscape() ? 14 : 15);
    }

    private int videoTitleHeightDp() {
        return tinyLandscape() ? 36 : (compactLandscape() ? 40 : 48);
    }

    private int videoTitleTopMarginDp() {
        return tinyLandscape() ? 3 : (compactLandscape() ? 4 : 6);
    }

    private int videoMetaSp() {
        return tinyLandscape() ? 10 : (compactLandscape() ? 11 : 12);
    }

    private int videoMetaHeightDp() {
        return tinyLandscape() ? 18 : (compactLandscape() ? 20 : 24);
    }
    private int detailHorizontalPaddingDp() {
        return tinyLandscape() ? 14 : (compactLandscape() ? 20 : 36);
    }

    private int detailVerticalPaddingDp() {
        return tinyLandscape() ? 8 : (compactLandscape() ? 14 : 28);
    }

    private int detailTopHeightDp() {
        return tinyLandscape() ? 42 : (compactLandscape() ? 50 : 64);
    }

    private int detailBackWidthDp() {
        return tinyLandscape() ? 86 : (compactLandscape() ? 96 : 110);
    }

    private int detailBackHeightDp() {
        return tinyLandscape() ? 34 : (compactLandscape() ? 40 : 48);
    }

    private int detailTitleSp() {
        return tinyLandscape() ? 20 : (compactLandscape() ? 23 : 28);
    }

    private int detailTitleHeightDp() {
        return tinyLandscape() ? 38 : (compactLandscape() ? 44 : 56);
    }

    private int detailTitleLeftMarginDp() {
        return tinyLandscape() ? 10 : (compactLandscape() ? 14 : 20);
    }

    private int detailPosterWidthDp() {
        return tinyLandscape() ? 118 : (compactLandscape() ? 164 : 260);
    }

    private int detailPosterHeightDp() {
        return tinyLandscape() ? 172 : (compactLandscape() ? 238 : 378);
    }

    private int detailRightMarginDp() {
        return tinyLandscape() ? 12 : (compactLandscape() ? 18 : 28);
    }

    private int detailMetaSp() {
        return tinyLandscape() ? 12 : (compactLandscape() ? 14 : 18);
    }

    private int detailMetaHeightDp() {
        return tinyLandscape() ? 28 : (compactLandscape() ? 36 : 54);
    }

    private int detailDescSp() {
        return tinyLandscape() ? 12 : (compactLandscape() ? 14 : 17);
    }

    private int detailDescLineSpacingDp() {
        return tinyLandscape() ? 0 : 2;
    }

    private int detailDescHeightDp() {
        return tinyLandscape() ? 36 : (compactLandscape() ? 58 : 118);
    }

    private int detailSectionTitleSp() {
        return tinyLandscape() ? 15 : (compactLandscape() ? 18 : 22);
    }

    private int detailSectionTitleHeightDp() {
        return tinyLandscape() ? 24 : (compactLandscape() ? 32 : 44);
    }

    private int detailSourceRowHeightDp() {
        return tinyLandscape() ? 36 : (compactLandscape() ? 44 : 58);
    }

    private int detailSourceTextSp() {
        return tinyLandscape() ? 12 : (compactLandscape() ? 14 : 16);
    }

    private int detailSourceButtonWidthDp() {
        return tinyLandscape() ? 86 : (compactLandscape() ? 104 : 126);
    }

    private int detailSourceButtonHeightDp() {
        return tinyLandscape() ? 30 : (compactLandscape() ? 36 : 46);
    }

    private int detailSourceHorizontalPaddingDp() {
        return tinyLandscape() ? 8 : (compactLandscape() ? 10 : 14);
    }

    private int detailSourceButtonGapDp() {
        return tinyLandscape() ? 6 : (compactLandscape() ? 8 : 10);
    }

    private int detailEpisodeSpacingDp() {
        return tinyLandscape() ? 6 : (compactLandscape() ? 8 : 12);
    }

    private int detailEpisodeTextSp() {
        return tinyLandscape() ? 13 : (compactLandscape() ? 15 : 17);
    }

    private int detailEpisodeHorizontalPaddingDp() {
        return tinyLandscape() ? 4 : (compactLandscape() ? 6 : 8);
    }

    private int detailEpisodeCellHeightDp() {
        return tinyLandscape() ? 36 : (compactLandscape() ? 44 : 54);
    }

    private void keepScreenImmersive() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        View decorView = getWindow().getDecorView();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            WindowInsetsController controller = decorView.getWindowInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            } else {
                applyLegacyImmersiveFlags(decorView);
            }
        } else {
            applyLegacyImmersiveFlags(decorView);
        }
    }

    private void applyLegacyImmersiveFlags(View decorView) {
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int key = event.getKeyCode();
            if (key == KeyEvent.KEYCODE_BACK) {
                handleBack();
                return true;
            }
            if ((screen == Screen.CATALOG || screen == Screen.SEARCH) && catalogGrid != null) {
                if (key == KeyEvent.KEYCODE_DPAD_UP
                        && screen == Screen.CATALOG
                        && PEACH_PATH.equals(currentPath)
                        && catalogGrid.hasFocus()) {
                    int position = catalogGrid.getSelectedItemPosition();
                    boolean firstRowSelected = position != AdapterView.INVALID_POSITION
                            && position < catalogGrid.getNumColumns();
                    if (firstRowSelected && catalogGrid.getFirstVisiblePosition() == 0) {
                        refreshPeachCatalogRandomly();
                        return true;
                    }
                }
                if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER) {
                    if (catalogGrid.hasFocus()) {
                        int position = catalogGrid.getSelectedItemPosition();
                        if (position == AdapterView.INVALID_POSITION && videoAdapter.getCount() > 0) {
                            position = 0;
                            catalogGrid.setSelection(0);
                        }
                        VideoItem item = videoAdapter.getItem(position);
                        if (item != null) {
                            loadDetail(item);
                            return true;
                        }
                    }
                }
                if (key == KeyEvent.KEYCODE_DPAD_LEFT && catalogGrid.hasFocus()) {
                    int position = catalogGrid.getSelectedItemPosition();
                    if (position == AdapterView.INVALID_POSITION || position % 5 == 0) {
                        if (activeNavButton != null) {
                            activeNavButton.requestFocus();
                        }
                        return true;
                    }
                }
                if (key == KeyEvent.KEYCODE_DPAD_RIGHT && navContainer != null && containsFocus(navContainer)) {
                    if (videoAdapter != null && videoAdapter.getCount() > 0) {
                        catalogGrid.requestFocus();
                        if (catalogGrid.getSelectedItemPosition() == AdapterView.INVALID_POSITION) {
                            catalogGrid.setSelection(0);
                        }
                    }
                    return true;
                }
            }
            if (screen == Screen.DETAIL && episodeGrid != null && episodeAdapter != null) {
                if (sourceScroll != null && sourceScroll.hasFocus()) {
                    if (key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        int offset = key == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : 1;
                        int next = activeSourcePosition + offset;
                        if (next >= 0 && next < currentSources.size()) {
                            selectSource(next);
                        }
                        return true;
                    }
                    if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER || key == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                        selectSource(activeSourcePosition);
                        if (episodeAdapter.getCount() > 0) {
                            episodeAdapter.forceSelectedPosition(0);
                            episodeGrid.requestFocus();
                            episodeGrid.setSelection(0);
                            refreshSourceRow();
                        }
                        return true;
                    }
                    if (key == KeyEvent.KEYCODE_DPAD_DOWN && episodeAdapter.getCount() > 0) {
                        int position = episodeGrid.getSelectedItemPosition();
                        if (position == AdapterView.INVALID_POSITION) {
                            position = 0;
                        }
                        episodeAdapter.forceSelectedPosition(position);
                        episodeGrid.requestFocus();
                        episodeGrid.setSelection(position);
                        refreshSourceRow();
                        return true;
                    }
                }
                if (episodeGrid.hasFocus() && key == KeyEvent.KEYCODE_DPAD_UP) {
                    int position = episodeGrid.getSelectedItemPosition();
                    if (position == AdapterView.INVALID_POSITION || position < 6) {
                        episodeAdapter.forceSelectedPosition(-1);
                        sourceScroll.requestFocus();
                        refreshSourceRow();
                        return true;
                    }
                }
                if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER || key == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                    if (episodeGrid.hasFocus()) {
                        int position = episodeGrid.getSelectedItemPosition();
                        if (position == AdapterView.INVALID_POSITION && episodeAdapter.getCount() > 0) {
                            position = 0;
                            episodeGrid.setSelection(0);
                        }
                        Episode episode = episodeAdapter.getItem(position);
                        if (episode != null) {
                            playEpisode(episode);
                            return true;
                        }
                    }
                }
            }
            if (screen == Screen.PLAYER && exoPlayer != null) {
                if (key == KeyEvent.KEYCODE_DPAD_LEFT) {
                    seekBy(-SEEK_STEP_MS);
                    return true;
                }
                if (key == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    seekBy(SEEK_STEP_MS);
                    return true;
                }
                if (key == KeyEvent.KEYCODE_MEDIA_REWIND) {
                    seekBy(-30_000L);
                    return true;
                }
                if (key == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
                    seekBy(30_000L);
                    return true;
                }
                if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER || key == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                    if (exoPlayer.isPlaying()) {
                        exoPlayer.pause();
                        showHint("暂停");
                    } else {
                        exoPlayer.play();
                        showHint("播放");
                    }
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void seekBy(long offsetMs) {
        if (exoPlayer == null) {
            return;
        }
        long duration = exoPlayer.getDuration();
        long current = Math.max(0L, exoPlayer.getCurrentPosition());
        long target = current + offsetMs;
        if (duration > 0) {
            target = Math.min(duration, Math.max(0L, target));
        } else {
            target = Math.max(0L, target);
        }
        exoPlayer.seekTo(target);
        if (playerView != null) {
            playerView.showController();
        }
        showHint((offsetMs > 0 ? "快进 " : "快退 ") + formatTime(target));
    }

    private static String formatTime(long positionMs) {
        long totalSeconds = Math.max(0L, positionMs / 1000L);
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0) {
            return String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }

    private boolean containsFocus(View view) {
        if (view == null) {
            return false;
        }
        if (view.hasFocus()) {
            return true;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (containsFocus(group.getChildAt(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    private void handleBack() {
        if (customView != null) {
            hideCustomView();
            return;
        }
        if (screen == Screen.PLAYER) {
            closePlayer();
            return;
        }
        if (screen == Screen.SEARCH) {
            showCatalog("首页", "/");
            return;
        }
        if (screen == Screen.SETTINGS) {
            showCatalog(currentTitle, currentPath);
            return;
        }
        if (screen == Screen.DETAIL) {
            showCatalog(currentTitle, currentPath);
            return;
        }
        finish();
    }

    private void closePlayer() {
        cleanupResolver();
        if (playerWebView != null) {
            playerWebView.destroy();
            playerWebView = null;
        }
        releaseNativePlayer();
        if (currentVideo != null) {
            loadDetail(currentVideo);
        } else {
            showCatalog(currentTitle, currentPath);
        }
    }

    private void hideCustomView() {
        if (customView == null) {
            return;
        }
        root.removeView(customView);
        customView = null;
        if (customViewCallback != null) {
            customViewCallback.onCustomViewHidden();
            customViewCallback = null;
        }
        if (playerWebView != null) {
            playerWebView.setVisibility(View.VISIBLE);
            playerWebView.requestFocus();
        }
        keepScreenImmersive();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (playerWebView != null) {
            playerWebView.onResume();
            playerWebView.resumeTimers();
        }
        if (playerStoppedForBackground && screen == Screen.PLAYER) {
            playerStoppedForBackground = false;
            closePlayer();
        }
        resumePendingUpdateInstallIfAllowed();
    }

    @Override
    protected void onPause() {
        pauseActivePlayback();
        super.onPause();
    }

    @Override
    protected void onStop() {
        stopActivePlayback();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        imageLoader.shutdown();
        cleanupResolver();
        releaseNativePlayer();
        releaseVideoCache();
        if (playerWebView != null) {
            playerWebView.destroy();
        }
        super.onDestroy();
    }

    private void pauseActivePlayback() {
        if (exoPlayer != null) {
            exoPlayer.pause();
        }
        if (playerWebView != null) {
            playerWebView.onPause();
            playerWebView.pauseTimers();
        }
    }

    private void stopActivePlayback() {
        boolean wasPlayingScreen = screen == Screen.PLAYER && (exoPlayer != null || playerWebView != null || playerView != null);
        cleanupResolver();
        releaseNativePlayer();
        if (playerWebView != null) {
            playerWebView.stopLoading();
            playerWebView.destroy();
            playerWebView = null;
        }
        if (customView != null) {
            customView = null;
            customViewCallback = null;
        }
        if (wasPlayingScreen) {
            playerStoppedForBackground = true;
        }
    }

    private final class PlayerChromeClient extends WebChromeClient {
        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            if (customView != null) {
                callback.onCustomViewHidden();
                return;
            }
            customView = view;
            customViewCallback = callback;
            if (playerWebView != null) {
                playerWebView.setVisibility(View.GONE);
            }
            root.addView(customView, matchParams());
            customView.requestFocus();
            keepScreenImmersive();
        }

        @Override
        public void onHideCustomView() {
            hideCustomView();
        }
    }

    private final class CaptureClient extends WebViewClient {
        @Override
        public void onPageFinished(WebView view, String url) {
            injectCaptureScript(view);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            if (url.contains(".m3u8")) {
                main.post(() -> onM3u8Captured(url, "WebView 请求"));
            }
            return super.shouldInterceptRequest(view, request);
        }
    }

    private final class M3u8Bridge {
        @JavascriptInterface
        public void report(String url, String source) {
            main.post(() -> onM3u8Captured(url, source == null ? "页面捕获" : source));
        }
    }

    private void injectCaptureScript(WebView view) {
        view.evaluateJavascript(M3U8_CAPTURE_SCRIPT, null);
    }

    private final class VideoAdapter extends BaseAdapter {
        private List<VideoItem> items;
        private int selectedPosition = -1;

        VideoAdapter(List<VideoItem> items) {
            this.items = items;
        }

        void setItems(List<VideoItem> items) {
            this.items = items;
            selectedPosition = items.isEmpty() ? -1 : 0;
            notifyDataSetChanged();
        }

        void appendItems(List<VideoItem> moreItems) {
            if (moreItems == null || moreItems.isEmpty()) {
                return;
            }
            LinkedHashMap<String, VideoItem> merged = new LinkedHashMap<>();
            for (VideoItem item : items) {
                merged.put(item.url, item);
            }
            for (VideoItem item : moreItems) {
                merged.put(item.url, item);
            }
            items = new ArrayList<>(merged.values());
            notifyDataSetChanged();
        }

        void setSelectedPosition(int selectedPosition) {
            if (this.selectedPosition == selectedPosition) {
                return;
            }
            this.selectedPosition = selectedPosition;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public VideoItem getItem(int position) {
            return position >= 0 && position < items.size() ? items.get(position) : null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            VideoCard card;
            if (convertView instanceof VideoCard) {
                card = (VideoCard) convertView;
            } else {
                card = new VideoCard();
            }
            card.bind(getItem(position), position == selectedPosition && catalogGrid != null && catalogGrid.hasFocus());
            return card;
        }
    }

    private final class VideoCard extends LinearLayout {
        private final ImageView poster;
        private final TextView title;
        private final TextView meta;

        VideoCard() {
            super(MainActivity.this);
            setOrientation(VERTICAL);
            setPadding(dp(cardPaddingDp()), dp(cardPaddingDp()), dp(cardPaddingDp()), dp(cardPaddingDp()));
            setFocusable(false);
            setBackgroundColor(PANEL);

            poster = new ImageView(MainActivity.this);
            poster.setScaleType(ImageView.ScaleType.CENTER_CROP);
            poster.setBackgroundColor(PANEL_2);
            addView(poster, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(videoPosterHeightDp())));

            title = label("", videoTitleSp(), TEXT, true);
            title.setMaxLines(2);
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(videoTitleHeightDp()));
            titleParams.topMargin = dp(videoTitleTopMarginDp());
            addView(title, titleParams);

            meta = label("", videoMetaSp(), MUTED, false);
            meta.setMaxLines(1);
            addView(meta, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(videoMetaHeightDp())));

        }

        void bind(VideoItem item, boolean selected) {
            if (item == null) {
                return;
            }
            title.setText(item.title);
            title.setTextColor(selected ? Color.BLACK : TEXT);
            meta.setText(item.remarks);
            meta.setTextColor(selected ? Color.BLACK : MUTED);
            setBackgroundColor(selected ? ACCENT : PANEL);
            imageLoader.load(item.poster, poster);
        }
    }

    private final class EpisodeAdapter extends BaseAdapter {
        private List<Episode> episodes;
        private int selectedPosition = -1;

        EpisodeAdapter(List<Episode> episodes) {
            this.episodes = episodes;
            this.selectedPosition = episodes.isEmpty() ? -1 : 0;
        }

        void setSelectedPosition(int selectedPosition) {
            if (this.selectedPosition == selectedPosition) {
                return;
            }
            this.selectedPosition = selectedPosition;
            notifyDataSetChanged();
        }

        void forceSelectedPosition(int selectedPosition) {
            this.selectedPosition = selectedPosition;
            notifyDataSetChanged();
        }

        void setEpisodes(List<Episode> episodes) {
            this.episodes = episodes == null ? new ArrayList<>() : episodes;
            selectedPosition = this.episodes.isEmpty() ? -1 : 0;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return episodes.size();
        }

        int indexOf(Episode episode) {
            return episodes.indexOf(episode);
        }

        @Override
        public Episode getItem(int position) {
            return position >= 0 && position < episodes.size() ? episodes.get(position) : null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView view = convertView instanceof TextView ? (TextView) convertView : label("", detailEpisodeTextSp(), TEXT, true);
            Episode episode = getItem(position);
            view.setText(episode == null ? "" : episode.displayTitle());
            view.setGravity(Gravity.CENTER);
            view.setPadding(dp(detailEpisodeHorizontalPaddingDp()), 0, dp(detailEpisodeHorizontalPaddingDp()), 0);
            boolean selected = position == selectedPosition && episodeGrid != null && episodeGrid.hasFocus();
            view.setTextColor(selected ? Color.BLACK : TEXT);
            view.setBackgroundColor(selected ? ACCENT : PANEL);
            view.setFocusable(false);
            view.setLayoutParams(new AdapterView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(detailEpisodeCellHeightDp())));
            return view;
        }
    }

    private final class ImageLoader {
        private final ExecutorService pool = Executors.newFixedThreadPool(3);
        private final Map<String, Bitmap> cache = Collections.synchronizedMap(new HashMap<>());

        void load(String src, ImageView target) {
            target.setImageDrawable(null);
            target.setTag(src);
            if (src == null || src.isEmpty()) {
                return;
            }
            Bitmap cached = cache.get(src);
            if (cached != null) {
                target.setImageBitmap(cached);
                return;
            }
            pool.execute(() -> {
                try {
                    Bitmap bitmap = loadBitmap(src);
                    if (bitmap != null) {
                        cache.put(src, bitmap);
                        main.post(() -> {
                            if (src.equals(target.getTag())) {
                                target.setImageBitmap(bitmap);
                            }
                        });
                    }
                } catch (Exception ignored) {
                }
            });
        }

        private Bitmap loadBitmap(String src) throws Exception {
            if (src.startsWith("data:")) {
                return decodeDataUrl(src);
            }
            String url = absolutize(src);
            if (url.contains(".image")) {
                String dataUrl = decodeEncryptedImage(url);
                return decodeDataUrl(dataUrl);
            }
            HttpURLConnection connection = openImageConnection(url);
            try (InputStream input = connection.getInputStream()) {
                return BitmapFactory.decodeStream(input);
            } finally {
                connection.disconnect();
            }
        }

        void shutdown() {
            pool.shutdownNow();
        }
    }

    private static final class SiteClient {
        private static final Pattern ANCHOR = Pattern.compile("(?is)<a\\b[^>]*>");
        private static final Pattern PLAY_LINK = Pattern.compile("(?is)<a\\b([^>]*)href=[\"']([^\"']*vod-play/([0-9]+)-(\\d+)-(\\d+)\\.html)[\"']([^>]*)>(.*?)</a>");

        List<VideoItem> fetchCatalog(String path) throws Exception {
            if (path != null && path.startsWith(PEACH_PATH)) {
                return fetchPeachCatalog(peachPage(path));
            }
            String html = fetch(absolutize(path), BASE_URL + "/");
            LinkedHashMap<String, VideoItem> out = new LinkedHashMap<>();
            Matcher matcher = ANCHOR.matcher(html);
            while (matcher.find()) {
                String tag = matcher.group();
                String href = attr(tag, "href");
                if (href == null || !href.contains("/vodhtml/")) {
                    continue;
                }
                String cls = attr(tag, "class");
                String title = attr(tag, "title");
                if ((title == null || title.isEmpty()) && cls != null && !cls.contains("hl-item-thumb") && !cls.contains("hl-br-thumb")) {
                    continue;
                }
                if (title == null || title.isEmpty()) {
                    title = cleanText(tag);
                }
                if (title.isEmpty()) {
                    continue;
                }
                String poster = attr(tag, "data-original");
                if (poster == null || poster.isEmpty()) {
                    poster = backgroundUrl(tag);
                }
                int liStart = Math.max(0, html.lastIndexOf("<li", matcher.start()));
                int liEnd = html.indexOf("</li>", matcher.end());
                String chunk = liEnd > liStart ? html.substring(liStart, Math.min(liEnd + 5, html.length())) : tag;
                String remarks = firstText(chunk, "remarks");
                String score = firstText(chunk, "score");
                String meta = score.isEmpty() ? remarks : score + "  " + remarks;
                String url = absolutize(href);
                if (!out.containsKey(url)) {
                    out.put(url, new VideoItem(title, url, poster, meta));
                }
            }
            return new ArrayList<>(out.values());
        }

        VideoDetail fetchDetail(VideoItem item) throws Exception {
            if (item != null && item.isPeach()) {
                return fetchPeachDetail(item);
            }
            String html = fetch(item.url, BASE_URL + "/");
            String title = item.title;
            String pageTitle = between(html, "<title>", "</title>");
            if (!pageTitle.isEmpty()) {
                int left = pageTitle.indexOf('《');
                int right = pageTitle.indexOf('》', left + 1);
                if (left >= 0 && right > left) {
                    title = pageTitle.substring(left + 1, right);
                }
            }
            String poster = firstMeta(html, "og:image");
            if (poster.isEmpty()) {
                poster = item.poster;
            }
            String desc = firstMeta(html, "description");
            String meta = collectMeta(html);
            Map<Integer, String> sourceNames = parseSourceNames(html);
            ArrayList<Episode> episodes = new ArrayList<>();
            LinkedHashMap<String, Episode> unique = new LinkedHashMap<>();
            Matcher matcher = PLAY_LINK.matcher(html);
            while (matcher.find()) {
                String href = matcher.group(2);
                int sid = parseInt(matcher.group(4));
                int nid = parseInt(matcher.group(5));
                String text = cleanText(matcher.group(7));
                if (text.isEmpty()) {
                    text = "第" + nid + "集";
                }
                if (sid > 1 && !text.contains("线路")) {
                    text = "线路" + sid + " " + text;
                }
                String path = absolutize(href);
                unique.put(path, new Episode(text, path, sid, nid, "", sourceNames.get(sid)));
            }
            episodes.addAll(unique.values());
            Collections.sort(episodes, (a, b) -> {
                int episodeNo = Integer.compare(a.index, b.index);
                if (episodeNo != 0) {
                    return episodeNo;
                }
                int priority = Integer.compare(a.sourcePriority(), b.sourcePriority());
                if (priority != 0) {
                    return priority;
                }
                return Integer.compare(a.source, b.source);
            });
            return new VideoDetail(title, poster, desc, meta, episodes);
        }

        private static Map<Integer, String> parseSourceNames(String html) {
            ArrayList<Integer> sourceIds = new ArrayList<>();
            Matcher linkMatcher = Pattern.compile("(?is)/vod-play/\\d+-(\\d+)-1\\.html").matcher(html);
            while (linkMatcher.find()) {
                int sid = parseInt(linkMatcher.group(1));
                if (sid > 0 && !sourceIds.contains(sid)) {
                    sourceIds.add(sid);
                }
            }

            ArrayList<String> names = new ArrayList<>();
            Matcher nameMatcher = Pattern.compile("(?is)<a\\b[^>]*class=[\"'][^\"']*(?:hl-from-btn|hl-tabs-btn)[^\"']*[\"'][^>]*>(.*?)</a>").matcher(html);
            while (nameMatcher.find()) {
                String name = cleanText(nameMatcher.group(1));
                if (!name.isEmpty() && !names.contains(name)) {
                    names.add(name);
                }
            }

            HashMap<Integer, String> out = new HashMap<>();
            int count = Math.min(sourceIds.size(), names.size());
            for (int i = 0; i < count; i++) {
                out.put(sourceIds.get(i), names.get(i));
            }
            return out;
        }

        PlayTarget resolvePlayTarget(Episode episode) throws Exception {
            if ("peach".equals(episode.from)) {
                return new PlayTarget(episode.title, episode.path, episode.path, "peach");
            }
            String playHtml = fetch(episode.path, BASE_URL + "/");
            String iframe = firstIframe(playHtml);
            String playerHtml = iframe.isEmpty() ? playHtml : fetch(absolutize(iframe), episode.path);
            String rawUrl = playerValue(playerHtml, "url");
            String from = playerValue(playerHtml, "from");
            String title = episode.title;
            if (!rawUrl.isEmpty()) {
                String decoded = rawUrl.replace("\\/", "/");
                if (decoded.startsWith("//")) {
                    decoded = "https:" + decoded;
                }
                if (isDirect(decoded)) {
                    return new PlayTarget(title, episode.path, decoded, from);
                }
            }
            return new PlayTarget(title, episode.path, "", from);
        }

        private List<VideoItem> fetchPeachCatalog(int page) throws Exception {
            String url = PEACH_API_BASE + "/api/vod/video?site_id=" + PEACH_SITE_ID
                    + "&channel_id=" + PEACH_CHANNEL_ID
                    + "&channel_name=" + Uri.encode(PEACH_CHANNEL_NAME)
                    + "&page=" + page
                    + "&per_page=24";
            JSONObject data = fetchPeachData(url, "");
            JSONArray items = data.optJSONArray("items");
            ArrayList<VideoItem> out = new ArrayList<>();
            if (items == null) {
                return out;
            }
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                String id = item.optString("id", "");
                String title = item.optString("name", "");
                String poster = peachImageUrl(item.optString("pic", ""));
                String playUrl = item.optString("play_url", "");
                String duration = item.optString("duration", "");
                String pubdate = item.optString("pubdate", "");
                String meta = duration.isEmpty() ? pubdate : duration + (pubdate.isEmpty() ? "" : "  " + pubdate);
                if (!id.isEmpty() && !title.isEmpty()) {
                    out.add(new VideoItem(title, PEACH_PATH + "/detail/" + id, poster, meta, "peach", id, playUrl));
                }
            }
            Collections.sort(out, (a, b) -> peachPubdate(b).compareTo(peachPubdate(a)));
            return out;
        }

        private static String peachPubdate(VideoItem item) {
            if (item == null || item.remarks == null || item.remarks.isEmpty()) {
                return "";
            }
            Matcher matcher = Pattern.compile("\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}|\\d{8}").matcher(item.remarks);
            if (matcher.find()) {
                return matcher.group().replace("/", "-");
            }
            return item.remarks;
        }

        private VideoDetail fetchPeachDetail(VideoItem item) throws Exception {
            JSONObject detail = item.remoteId.isEmpty() ? null : fetchPeachData(PEACH_API_BASE + "/api/vod/video/" + item.remoteId
                    + "?site_id=" + PEACH_SITE_ID
                    + "&channel_id=" + PEACH_CHANNEL_ID
                    + "&channel_name=" + Uri.encode(PEACH_CHANNEL_NAME), "");
            JSONObject data = detail == null ? new JSONObject() : detail;
            String title = valueOr(data.optString("name", ""), item.title);
            String poster = peachImageUrl(valueOr(data.optString("pic", ""), item.poster));
            String playPath = valueOr(data.optString("play_url", ""), item.playUrl);
            List<String> playUrls = peachPlayUrls(playPath);
            String desc = valueOr(data.optString("description", ""), title);
            String duration = data.optString("duration", "");
            String pubdate = data.optString("pubdate", "");
            String meta = duration.isEmpty() ? pubdate : duration + (pubdate.isEmpty() ? "" : "  " + pubdate);
            ArrayList<Episode> episodes = new ArrayList<>();
            for (int i = 0; i < playUrls.size(); i++) {
                episodes.add(new Episode("播放", playUrls.get(i), i + 1, 1, "peach", "线路" + (i + 1)));
            }
            return new VideoDetail(title, poster, desc, meta, episodes);
        }

        private static JSONObject fetchPeachData(String url, String referer) throws Exception {
            String raw = fetch(url, referer);
            JSONObject payload = new JSONObject(raw);
            String encrypted = payload.optString("x-data", "");
            JSONObject body = encrypted.isEmpty() ? payload : new JSONObject(fernetDecrypt(encrypted, PEACH_FERNET_KEY));
            if (body.optInt("code", 0) != 0) {
                throw new IllegalStateException(body.optString("message", "API error"));
            }
            JSONObject data = body.optJSONObject("data");
            return data == null ? new JSONObject() : data;
        }

        private static int peachPage(String path) {
            if (path == null) {
                return 1;
            }
            int index = path.indexOf("page=");
            if (index < 0) {
                return 1;
            }
            return Math.max(1, parseInt(path.substring(index + 5).replaceAll("[^0-9].*$", "")));
        }

        private static String peachImageUrl(String path) {
            return absolutizeHost(PEACH_IMAGE_HOST, path);
        }

        private static List<String> peachPlayUrls(String path) {
            ArrayList<String> out = new ArrayList<>();
            for (String host : PEACH_PLAY_HOSTS) {
                String url = absolutizeHost(host, path);
                if (!url.isEmpty()) {
                    out.add(url);
                }
            }
            return out;
        }

        private static String absolutizeHost(String host, String path) {
            if (path == null || path.isEmpty()) {
                return "";
            }
            if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("data:")) {
                return path;
            }
            return host + (path.startsWith("/") ? path : "/" + path);
        }

        private static String valueOr(String value, String fallback) {
            return value == null || value.isEmpty() ? (fallback == null ? "" : fallback) : value;
        }

        private static String fetch(String url, String referer) throws Exception {
            HttpURLConnection connection = openConnection(url);
            connection.setRequestProperty("Referer", referer);
            StringBuilder builder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append('\n');
                }
            } finally {
                connection.disconnect();
            }
            return builder.toString();
        }

        private static String attr(String tag, String name) {
            Matcher matcher = Pattern.compile("(?is)\\b" + Pattern.quote(name) + "\\s*=\\s*([\"'])(.*?)\\1").matcher(tag);
            return matcher.find() ? htmlDecode(matcher.group(2).trim()) : "";
        }

        private static String backgroundUrl(String tag) {
            Matcher matcher = Pattern.compile("(?is)background-image\\s*:\\s*url\\((['\"]?)(.*?)\\1\\)").matcher(tag);
            return matcher.find() ? matcher.group(2).trim() : "";
        }

        private static String firstText(String html, String cls) {
            Matcher matcher = Pattern.compile("(?is)<[^>]+class=[\"'][^\"']*" + Pattern.quote(cls) + "[^\"']*[\"'][^>]*>(.*?)</[^>]+>").matcher(html);
            return matcher.find() ? cleanText(matcher.group(1)) : "";
        }

        private static String firstMeta(String html, String name) {
            Matcher matcher = Pattern.compile("(?is)<meta\\b(?=[^>]*(?:name|property)=[\"']" + Pattern.quote(name) + "[\"'])[^>]*content=[\"'](.*?)[\"'][^>]*>").matcher(html);
            return matcher.find() ? htmlDecode(matcher.group(1).trim()) : "";
        }

        private static String collectMeta(String html) {
            String status = field(html, "状态：");
            String actor = field(html, "主演：");
            String year = field(html, "年份：");
            String type = field(html, "类型：");
            StringBuilder builder = new StringBuilder();
            if (!status.isEmpty()) builder.append(status);
            if (!year.isEmpty()) appendMeta(builder, year);
            if (!type.isEmpty()) appendMeta(builder, type);
            if (!actor.isEmpty()) appendMeta(builder, actor);
            return builder.length() == 0 ? "来自爱壹帆" : builder.toString();
        }

        private static void appendMeta(StringBuilder builder, String text) {
            if (builder.length() > 0) builder.append(" / ");
            builder.append(text);
        }

        private static String field(String html, String label) {
            int index = html.indexOf(label);
            if (index < 0) {
                return "";
            }
            int end = html.indexOf('\n', index);
            String chunk = end > index ? html.substring(index, Math.min(end, index + 220)) : html.substring(index, Math.min(html.length(), index + 220));
            chunk = cleanText(chunk).replace(label, "").trim();
            int cut = chunk.indexOf(" ");
            return cut > 0 ? chunk.substring(0, cut) : chunk;
        }

        private static String firstIframe(String html) {
            Matcher matcher = Pattern.compile("(?is)<iframe\\b[^>]*id=[\"']player_if[\"'][^>]*src=[\"']([^\"']+)[\"']").matcher(html);
            if (matcher.find()) {
                return matcher.group(1);
            }
            matcher = Pattern.compile("(?is)<iframe\\b[^>]*src=[\"']([^\"']*/vod/player/[^\"']+)[\"']").matcher(html);
            return matcher.find() ? matcher.group(1) : "";
        }

        private static String playerValue(String html, String key) {
            Matcher matcher = Pattern.compile("(?is)player_aaaa\\s*=\\s*\\{.*?[\"']" + Pattern.quote(key) + "[\"']\\s*:\\s*[\"'](.*?)[\"']").matcher(html);
            return matcher.find() ? matcher.group(1) : "";
        }

        private static boolean isDirect(String url) {
            String lower = url.toLowerCase(Locale.ROOT);
            return lower.contains(".m3u8") || lower.contains(".mp4") || lower.contains(".flv") || lower.contains(".webm");
        }

        private static String between(String html, String start, String end) {
            int left = html.indexOf(start);
            if (left < 0) return "";
            int right = html.indexOf(end, left + start.length());
            if (right < 0) return "";
            return htmlDecode(html.substring(left + start.length(), right).trim());
        }

        private static String cleanText(String html) {
            String noTags = html.replaceAll("(?is)<script.*?</script>", " ")
                    .replaceAll("(?is)<style.*?</style>", " ")
                    .replaceAll("(?is)<[^>]+>", " ");
            return htmlDecode(noTags).replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
        }

        private static String htmlDecode(String text) {
            if (text == null) {
                return "";
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY).toString();
            }
            return Html.fromHtml(text).toString();
        }

        private static int parseInt(String value) {
            try {
                return Integer.parseInt(value);
            } catch (Exception ignored) {
                return 0;
            }
        }
    }

    private static HttpURLConnection openConnection(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(6000);
        connection.setReadTimeout(8000);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android TV) AppleWebKit/537.36 YfVodTVNative/1.0");
        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml,image/avif,image/webp,image/*,*/*;q=0.8");
        return connection;
    }

    private static HttpURLConnection openImageConnection(String url) throws Exception {
        HttpURLConnection connection = openImageConnection(url, false);
        try {
            connection.getResponseCode();
            return connection;
        } catch (SSLHandshakeException e) {
            connection.disconnect();
            return openImageConnection(url, true);
        }
    }

    private static HttpURLConnection openImageConnection(String url, boolean trustAll) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(6000);
        connection.setReadTimeout(8000);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android TV) AppleWebKit/537.36 YfVodTVNative/1.0");
        connection.setRequestProperty("Accept", "image/jpeg,image/png,image/webp,image/*,*/*;q=0.8");
        connection.setRequestProperty("Referer", BASE_URL + "/");
        if (trustAll && connection instanceof HttpsURLConnection) {
            HttpsURLConnection https = (HttpsURLConnection) connection;
            https.setSSLSocketFactory(trustAllSslContext().getSocketFactory());
            https.setHostnameVerifier(trustAllHostnameVerifier());
        }
        return connection;
    }

    private static SSLContext trustAllSslContext() throws Exception {
        TrustManager[] trustManagers = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
        };
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, trustManagers, new SecureRandom());
        return context;
    }

    private static HostnameVerifier trustAllHostnameVerifier() {
        return (hostname, session) -> true;
    }

    private static String decodeEncryptedImage(String url) throws Exception {
        String text = readText(url, "");
        String dataUrl;
        int split = text.indexOf("@@@");
        if (split >= 0) {
            dataUrl = fernetDecrypt(text.substring(0, split), PEACH_FERNET_KEY) + text.substring(split + 3);
        } else {
            dataUrl = fernetDecrypt(text.trim(), PEACH_FERNET_KEY);
        }
        if (!dataUrl.startsWith("data:")) {
            dataUrl = "data:image/jpeg;base64," + dataUrl;
        }
        return dataUrl;
    }

    private static Bitmap decodeDataUrl(String dataUrl) {
        int comma = dataUrl.indexOf(',');
        String payload = comma >= 0 ? dataUrl.substring(comma + 1) : dataUrl;
        byte[] bytes = Base64.decode(payload, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    private static String readText(String url, String referer) throws Exception {
        HttpURLConnection connection = openConnection(url);
        if (referer != null && !referer.isEmpty()) {
            connection.setRequestProperty("Referer", referer);
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        } finally {
            connection.disconnect();
        }
        return builder.toString();
    }

    private static String fernetDecrypt(String token, String key) throws Exception {
        byte[] tokenBytes = base64UrlDecode(token);
        byte[] keyBytes = base64UrlDecode(key);
        if (tokenBytes.length < 57 || keyBytes.length != 32 || tokenBytes[0] != (byte) 0x80) {
            throw new IllegalArgumentException("Invalid Fernet token");
        }
        byte[] signingKey = new byte[16];
        byte[] encryptionKey = new byte[16];
        System.arraycopy(keyBytes, 0, signingKey, 0, 16);
        System.arraycopy(keyBytes, 16, encryptionKey, 0, 16);

        int signatureStart = tokenBytes.length - 32;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
        byte[] expected = mac.doFinal(java.util.Arrays.copyOfRange(tokenBytes, 0, signatureStart));
        byte[] actual = java.util.Arrays.copyOfRange(tokenBytes, signatureStart, tokenBytes.length);
        if (!java.security.MessageDigest.isEqual(expected, actual)) {
            throw new SecurityException("Invalid Fernet signature");
        }

        byte[] iv = java.util.Arrays.copyOfRange(tokenBytes, 9, 25);
        byte[] cipherText = java.util.Arrays.copyOfRange(tokenBytes, 25, signatureStart);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"), new IvParameterSpec(iv));
        return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
    }

    private static byte[] base64UrlDecode(String value) {
        String normalized = value.trim();
        int padding = (4 - normalized.length() % 4) % 4;
        for (int i = 0; i < padding; i++) {
            normalized += "=";
        }
        return Base64.decode(normalized, Base64.URL_SAFE | Base64.NO_WRAP);
    }

    private static final String M3U8_CAPTURE_SCRIPT =
            "(function(){"
                    + "if(window.__yfvodCaptureInjected){return;}window.__yfvodCaptureInjected=true;"
                    + "function abs(u){try{return new URL(u,location.href).href;}catch(e){return '';}}"
                    + "function report(u,s){if(!u||u.indexOf('.m3u8')<0){return;}u=abs(u);if(!u){return;}try{YfVodCapture.report(u,s||'JS捕获');}catch(e){}}"
                    + "function parse(txt,base){try{if(!txt||txt.indexOf('#EXT-X-STREAM-INF')<0){return;}txt.split('\\n').forEach(function(line){line=line.trim();if(line&&line.charAt(0)!=='#'&&line.indexOf('.m3u8')>=0){report(new URL(line,base).href,'Master子链接');}});}catch(e){}}"
                    + "var open=XMLHttpRequest.prototype.open;var send=XMLHttpRequest.prototype.send;"
                    + "XMLHttpRequest.prototype.open=function(m,u){this.__yfvodUrl=u;return open.apply(this,arguments);};"
                    + "XMLHttpRequest.prototype.send=function(){var u=this.__yfvodUrl;if(typeof u==='string'&&u.indexOf('.m3u8')>=0){this.addEventListener('load',function(){var full=this.responseURL||u;report(full,'XHR');try{var c='';if(this.responseType==='arraybuffer'&&this.response){c=new TextDecoder('utf-8').decode(this.response);}else if(!this.responseType||this.responseType==='text'){c=this.responseText;}parse(c,full);}catch(e){}});}return send.apply(this,arguments);};"
                    + "if(window.fetch){var oldFetch=window.fetch;window.fetch=function(input,init){return oldFetch.apply(this,arguments).then(function(resp){try{var u=typeof input==='string'?input:input.url;if(u&&u.indexOf('.m3u8')>=0){report(u,'Fetch');resp.clone().text().then(function(t){parse(t,abs(u));}).catch(function(){});}}catch(e){}return resp;});};}"
                    + "try{new PerformanceObserver(function(list){list.getEntries().forEach(function(e){if(e.name&&e.name.indexOf('.m3u8')>=0){report(e.name,'Performance');}});}).observe({entryTypes:['resource']});}catch(e){}"
                    + "setInterval(function(){try{if(window.player_aaaa&&window.player_aaaa.url){report(window.player_aaaa.url,'player_aaaa');}}catch(e){}},600);"
                    + "try{if(window.player_aaaa&&window.player_aaaa.url){report(window.player_aaaa.url,'player_aaaa');}}catch(e){}"
                    + "})();";

    private static final class Category {
        final String name;
        final String path;

        Category(String name, String path) {
            this.name = name;
            this.path = path;
        }
    }

    private static final class UpdateInfo {
        final String versionName;
        final int versionCode;
        final String apkName;
        final String apkUrl;

        UpdateInfo(String versionName, int versionCode, String apkName, String apkUrl) {
            this.versionName = versionName == null ? "" : versionName;
            this.versionCode = versionCode;
            this.apkName = apkName == null ? "" : apkName;
            this.apkUrl = apkUrl == null ? "" : apkUrl;
        }
    }

    private static class VideoItem {
        final String title;
        final String url;
        final String poster;
        final String remarks;
        final String provider;
        final String remoteId;
        final String playUrl;

        VideoItem(String title, String url, String poster, String remarks) {
            this(title, url, poster, remarks, "", "", "");
        }

        VideoItem(String title, String url, String poster, String remarks, String provider, String remoteId, String playUrl) {
            this.title = title;
            this.url = url;
            this.poster = poster;
            this.remarks = remarks;
            this.provider = provider == null ? "" : provider;
            this.remoteId = remoteId == null ? "" : remoteId;
            this.playUrl = playUrl == null ? "" : playUrl;
        }

        boolean isPeach() {
            return "peach".equals(provider);
        }
    }

    private static final class VideoDetail {
        final String title;
        final String poster;
        final String description;
        final String meta;
        final List<Episode> episodes;

        VideoDetail(String title, String poster, String description, String meta, List<Episode> episodes) {
            this.title = title;
            this.poster = poster;
            this.description = description == null || description.isEmpty() ? "暂无简介" : description;
            this.meta = meta;
            this.episodes = episodes;
        }
    }

    private static final class SourceGroup {
        final int source;
        final String title;
        final ArrayList<Episode> episodes = new ArrayList<>();

        SourceGroup(int source, String title) {
            this.source = source;
            this.title = title == null || title.isEmpty() ? "线路" + source : title;
        }

        int priority() {
            if (title.contains("国际") || title.contains("亚太") || title.contains("备用") || title.contains("海外") || title.contains("m3u8")) {
                return 0;
            }
            if (title.contains("高清")) {
                return 5;
            }
            return 2;
        }
    }

    private static final class Episode {
        final String title;
        final String path;
        final int source;
        final int index;
        final String from;
        final String sourceName;

        Episode(String title, String path, int source, int index, String from, String sourceName) {
            this.title = title;
            this.path = path;
            this.source = source;
            this.index = index;
            this.from = from == null ? "" : from;
            this.sourceName = sourceName == null ? "" : sourceName;
        }

        int priority() {
            if (isPreferredNativeSource(from)) {
                return 0;
            }
            if (from.contains("m3u8") || from.equals("wolong") || from.equals("360zy") || from.equals("dplayer") || from.equals("haiwaikan")) {
                return 1;
            }
            return 5;
        }

        int sourcePriority() {
            if (!from.isEmpty()) {
                return priority();
            }
            String name = sourceName;
            if (name.contains("国际") || name.contains("亚太") || name.contains("备用") || name.contains("海外")) {
                return 0;
            }
            if (name.contains("高清")) {
                return 5;
            }
            return 2;
        }

        boolean isNativePreferred() {
            return priority() <= 1;
        }

        String displayTitle() {
            return title;
        }

        String sourceLabel() {
            if (!sourceName.isEmpty()) {
                return sourceName;
            }
            if (!from.isEmpty()) {
                return priority() <= 1 ? "m3u8" : from;
            }
            return "线路" + source;
        }

        static boolean isPreferredNativeSource(String from) {
            if (from == null) {
                return false;
            }
            return from.contains("m3u8") || from.equals("wolong") || from.equals("360zy") || from.equals("kuaikan") || from.equals("leshi") || from.equals("hw8") || from.equals("haiwaikan") || from.equals("dplayer");
        }
    }

    private static final class PlayTarget {
        final String title;
        final String webUrl;
        final String directUrl;
        final String from;

        PlayTarget(String title, String webUrl, String directUrl, String from) {
            this.title = title;
            this.webUrl = webUrl;
            this.directUrl = directUrl;
            this.from = from == null ? "" : from;
        }

        boolean isDirectVideo() {
            return directUrl != null && !directUrl.isEmpty();
        }
    }
}

