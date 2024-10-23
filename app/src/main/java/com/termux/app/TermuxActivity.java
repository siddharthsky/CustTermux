package com.termux.app;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.text.InputFilter;
import android.text.InputType;
import android.text.format.Formatter;
import android.util.Base64;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.view.autofill.AutofillManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.webkit.WebView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.cast.framework.CastSession;
import com.termux.AppSelectorActivity;
import com.termux.BuildConfig;
import com.termux.SkyActionActivity;
import com.termux.Utils2;
import com.termux.app.sky.SkyTVz;
import com.termux.setup_login.LoginActivity2;
import com.termux.LoginStatusChecker;
import com.termux.R;
import com.termux.ServerStatusChecker;
import com.termux.TermuxActivityResume;
import com.termux.Utils;
import com.termux.app.activities.HelpActivity;
import com.termux.app.activities.SettingsActivity;
import com.termux.app.api.file.FileReceiverActivity;
import com.termux.app.terminal.TermuxActivityRootView;
import com.termux.app.terminal.TermuxSessionsListViewController;
import com.termux.app.terminal.TermuxTerminalSessionActivityClient;
import com.termux.app.terminal.TermuxTerminalViewClient;
import com.termux.app.terminal.io.TerminalToolbarViewPager;
import com.termux.app.terminal.io.TermuxTerminalExtraKeys;
import com.termux.setup.SetupActivity;
import com.termux.SkySharedPref;
import com.termux.WebPlayerActivity;
import com.termux.setup_app.SetupActivityApp;
import com.termux.shared.activities.ReportActivity;
import com.termux.shared.activity.ActivityUtils;
import com.termux.shared.activity.media.AppCompatActivityUtils;
import com.termux.shared.data.IntentUtils;
import com.termux.shared.android.PermissionUtils;
import com.termux.shared.data.DataUtils;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_ACTIVITY;
import com.termux.shared.termux.crash.TermuxCrashUtils;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.extrakeys.ExtraKeysView;
import com.termux.shared.termux.interact.TextInputDialogUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxUtils;
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties;
import com.termux.shared.termux.theme.TermuxThemeUtils;
import com.termux.shared.theme.NightMode;
import com.termux.shared.view.ViewUtils;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSessionClient;
import com.termux.tv_ui.TVPlayer;
import com.termux.view.TerminalView;
import com.termux.view.TerminalViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.mediarouter.app.MediaRouteButton;
import androidx.viewpager.widget.ViewPager;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Objects;

/**
 * A terminal emulator activity.
 * <p/>
 * See
 * <ul>
 * <li>http://www.mongrel-phones.com.au/default/how_to_make_a_local_service_and_bind_to_it_in_android</li>
 * <li>https://code.google.com/p/android/issues/detail?id=6426</li>
 * </ul>
 * about memory leaks.
 */
public final class TermuxActivity extends AppCompatActivity implements ServiceConnection {

    /**
     * The connection to the {@link TermuxService}. Requested in {@link #onCreate(Bundle)} with a call to
     * {@link #bindService(Intent, ServiceConnection, int)}, and obtained and stored in
     * {@link #onServiceConnected(ComponentName, IBinder)}.
     */
    TermuxService mTermuxService;

    /**
     * The {@link TerminalView} shown in  {@link TermuxActivity} that displays the terminal.
     */
    TerminalView mTerminalView;

    /**
     *  The {@link TerminalViewClient} interface implementation to allow for communication between
     *  {@link TerminalView} and {@link TermuxActivity}.
     */
    TermuxTerminalViewClient mTermuxTerminalViewClient;

    /**
     *  The {@link TerminalSessionClient} interface implementation to allow for communication between
     *  {@link TerminalSession} and {@link TermuxActivity}.
     */
    TermuxTerminalSessionActivityClient mTermuxTerminalSessionActivityClient;

    /**
     * Termux app shared preferences manager.
     */
    private TermuxAppSharedPreferences mPreferences;

    /**
     * Termux app SharedProperties loaded from termux.properties
     */
    private TermuxAppSharedProperties mProperties;

    /**
     * The root view of the {@link TermuxActivity}.
     */
    TermuxActivityRootView mTermuxActivityRootView;

    /**
     * The space at the bottom of {@link @mTermuxActivityRootView} of the {@link TermuxActivity}.
     */
    View mTermuxActivityBottomSpaceView;

    /**
     * The terminal extra keys view.
     */
    ExtraKeysView mExtraKeysView;

    /**
     * The client for the {@link #mExtraKeysView}.
     */
    TermuxTerminalExtraKeys mTermuxTerminalExtraKeys;

    /**
     * The termux sessions list controller.
     */
    TermuxSessionsListViewController mTermuxSessionListViewController;

    /**
     * The {@link TermuxActivity} broadcast receiver for various things like terminal style configuration changes.
     */
    private final BroadcastReceiver mTermuxActivityBroadcastReceiver = new TermuxActivityBroadcastReceiver();

    /**
     * The last toast shown, used cancel current toast before showing new in {@link #showToast(String, boolean)}.
     */
    Toast mLastToast;

    /**
     * If between onResume() and onStop(). Note that only one session is in the foreground of the terminal view at the
     * time, so if the session causing a change is not in the foreground it should probably be treated as background.
     */
    private boolean mIsVisible;

    /**
     * If onResume() was called after onCreate().
     */
    private boolean mIsOnResumeAfterOnCreate = false;

    /**
     * If activity was restarted like due to call to {@link #recreate()} after receiving
     * {@link TERMUX_ACTIVITY#ACTION_RELOAD_STYLE}, system dark night mode was changed or activity
     * was killed by android.
     */
    private boolean mIsActivityRecreated = false;

    /**
     * The {@link TermuxActivity} is in an invalid state and must not be run.
     */
    private boolean mIsInvalidState;

    private int mNavBarHeight;

    private float mTerminalToolbarDefaultHeight;


    private static final int CONTEXT_MENU_SELECT_URL_ID = 0;
    private static final int CONTEXT_MENU_SHARE_TRANSCRIPT_ID = 1;
    private static final int CONTEXT_MENU_SHARE_SELECTED_TEXT = 10;
    private static final int CONTEXT_MENU_AUTOFILL_ID = 2;
    private static final int CONTEXT_MENU_RESET_TERMINAL_ID = 3;
    private static final int CONTEXT_MENU_KILL_PROCESS_ID = 4;
    private static final int CONTEXT_MENU_STYLING_ID = 5;
    private static final int CONTEXT_MENU_TOGGLE_KEEP_SCREEN_ON = 6;
    private static final int CONTEXT_MENU_HELP_ID = 7;
    private static final int CONTEXT_MENU_SETTINGS_ID = 8;
    private static final int CONTEXT_MENU_REPORT_ID = 9;

    private static final String ARG_TERMINAL_TOOLBAR_TEXT_INPUT = "terminal_toolbar_text_input";
    private static final String ARG_ACTIVITY_RECREATED = "activity_recreated";

    private static final String LOG_TAG = "TermuxActivity";

    private Handler handler;

    private Runnable runnable;

    private String phoneNumber;

    private String otp;

    private WebView webView;

    private ImageView imageView;

    private boolean isWebViewVisible = false;

    private static final int REQUEST_CODE_MANAGE_OVERLAY_PERMISSION = 1;
    private static final int REQUEST_CODE_INSTALL_PACKAGES_PERMISSION = 2;

    private ImageView downloadIcon;
    private ImageView copyIcon;

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static String DOWNLOAD_URL;

    private TextView ipAddressTextView;
    private TextView textplay;
    private TextView serverStatusTextView;
    private ServerStatusChecker serverStatusChecker;
    private LoginStatusChecker loginStatusChecker;

    private boolean isCanceled = false;

    private Runnable autoDismissRunnable;

    private AlertDialog alertDialog;

    private TermuxActivityResume termuxActivityResume;

    private Runnable stopRunnable;
    private String urlport;
    private String urlportonly;
    private String downloadUrl;
    private String copy_text;
    private String ipport;

//    private CastHelper castHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Logger.logDebug(LOG_TAG, "onCreate");
        mIsOnResumeAfterOnCreate = true;

        // Initialize SkySharedPref and other member variables
        SkySharedPref preferenceManager = new SkySharedPref(this);
        DOWNLOAD_URL = preferenceManager.getKey("isLocalPORT");



        if (savedInstanceState != null)
            mIsActivityRecreated = savedInstanceState.getBoolean(ARG_ACTIVITY_RECREATED, false);

        // Delete ReportInfo serialized object files from cache older than 14 days
        ReportActivity.deleteReportInfoFilesOlderThanXDays(this, 14, false);

        // Load Termux app SharedProperties from disk
        mProperties = TermuxAppSharedProperties.getProperties();
        reloadProperties();

        setActivityTheme();

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_termux);
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

//        // Initialize CastHelper
//        castHelper = new CastHelper(this);
//
//        // Setup MediaRouteButton
//        MediaRouteButton mediaRouteButton = findViewById(R.id.media_route_button);
//        castHelper.setupMediaRouteButton(mediaRouteButton);



        TextView serverStatusTextView = findViewById(R.id.server_status);
        TextView loginStatusTextView = findViewById(R.id.login_status);
        serverStatusChecker = new ServerStatusChecker(TermuxActivity.this, serverStatusTextView);
        loginStatusChecker = new LoginStatusChecker(TermuxActivity.this, loginStatusTextView);



        termuxActivityResume = new TermuxActivityResume(this);

        Button button1 = findViewById(R.id.button1);
        Button button1_5 = findViewById(R.id.button1_5);
        Button button2 = findViewById(R.id.button2);
        Button button3 = findViewById(R.id.button3);
//        Button button4 = findViewById(R.id.button4);
//        Button button5 = findViewById(R.id.button5);
        Button button6 = findViewById(R.id.button6);
        Button button6_5 = findViewById(R.id.button6_5);
        Button button7 = findViewById(R.id.button7);
        Button button8 = findViewById(R.id.button8);
        ImageView downloadIconx = findViewById(R.id.ic_download);
        ImageView copyIcon = findViewById(R.id.ic_copycat);
        ImageView iptvIcon = findViewById(R.id.ic_iptvIcon);

        button1.requestFocus();

        View.OnFocusChangeListener tooltipFocusListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    //for (int i = 0; i < 3; i++) {
                    v.performLongClick();
                    //}
                }
            }
        };

        button1.setOnFocusChangeListener(tooltipFocusListener);
//        button1_5.setOnFocusChangeListener(tooltipFocusListener);
        button2.setOnFocusChangeListener(tooltipFocusListener);
//        button3.setOnFocusChangeListener(tooltipFocusListener);
//        button4.setOnFocusChangeListener(tooltipFocusListener);
//        button5.setOnFocusChangeListener(tooltipFocusListener);
        button6.setOnFocusChangeListener(tooltipFocusListener);
        button6_5.setOnFocusChangeListener(tooltipFocusListener);
        button7.setOnFocusChangeListener(tooltipFocusListener);
        button8.setOnFocusChangeListener(tooltipFocusListener);
        downloadIconx.setOnFocusChangeListener(tooltipFocusListener);
        copyIcon.setOnFocusChangeListener(tooltipFocusListener);

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sky_rerun();
            }
        });

        button1_5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TermuxActivity.this, WebPlayerActivity.class);
                startActivity(intent);
            }
        });

        button1_5.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                //Utils.lake_alert_WEBTV(TermuxActivity.this);
                Utils2.lake_alert_WEBTV(TermuxActivity.this);
                return false;
            }
        });

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(TermuxActivity.this, LoginActivity2.class);
                startActivity(intent);
            }
        });

        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle button3 click
                SkySharedPref preferenceManager = new SkySharedPref(TermuxActivity.this);
                String apppkg = preferenceManager.getKey("app_name");
                String appclass = preferenceManager.getKey("app_launchactivity");

                if (apppkg != null && !apppkg.isEmpty()) {
                    try {
                        if (apppkg.equals("null")) {
                            System.out.println("A11");
                            Toast.makeText(TermuxActivity.this, "IPTV is not set up. Please setup.", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(TermuxActivity.this, AppSelectorActivity.class);
                            startActivity(intent);
                        } else if (apppkg.equals("sky_web_tv")) {
                            System.out.println("A12");
                            Intent intent = new Intent(TermuxActivity.this, WebPlayerActivity.class);
                            startActivity(intent);
                        } else {
                            System.out.println("A13");
                            Intent intent = new Intent();
                            intent.setComponent(new ComponentName(apppkg, appclass));
                            startActivity(intent);
                        }
                    } catch (ActivityNotFoundException e) {
                        System.out.println("Unable to open the specified app.");
                        Toast.makeText(TermuxActivity.this, "Unable to open the specified app.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });


        button3.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Intent intent = new Intent(TermuxActivity.this, AppSelectorActivity.class);
                startActivity(intent);
                return true;
            }
        });

        ButtonClick6ListenerUtil.setButtonClickListener(TermuxActivity.this, button6);


        ButtonClick6_5ListenerUtil.setButtonClickListener(TermuxActivity.this, button6_5);


//        button6_5.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(TermuxActivity.this, WebPlayerActivity.class);
//                startActivity(intent);
//            }
//        });


        button7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sky_exit();
//                Intent intent = new Intent(TermuxActivity.this, AnotherActivityCast.class);
//                startActivity(intent);
            }
        });


        button8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sky_config();
            }
        });


        imageView = findViewById(R.id.imageView);
        //webView = findViewById(R.id.webview_tv);

        //webView.setVisibility(View.GONE); // Initially hide the WebView
        imageView.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetJavaScriptEnabled")
            @Override
            public void onClick(View v) {
//                if (isWebViewVisible) {
//                    webView.setVisibility(View.GONE);
//                } else {
//                    webView.setVisibility(View.VISIBLE);
//                    webView.setWebChromeClient(new WebChromeClient());
//
//                    WebSettings webSettings = webView.getSettings();
//                    webSettings.setJavaScriptEnabled(true);
//                    webView.loadUrl("http://localhost:5001");
//                }
//                isWebViewVisible = !isWebViewVisible;

                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(DOWNLOAD_URL));
                startActivity(browserIntent);
            }
        });

        String permissionRequestCountStr = preferenceManager.getKey("permissionRequestCount");

        int permissionRequestCount = 0;
        if (permissionRequestCountStr != null && !permissionRequestCountStr.isEmpty()) {
            permissionRequestCount = Integer.parseInt(permissionRequestCountStr);
        }

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (!Settings.canDrawOverlays(this)) {
//                if (permissionRequestCount < 2) {
//                    overapp_confirmation(this);
//                    permissionRequestCount++;
//                    preferenceManager.setKey("permissionRequestCount", String.valueOf(permissionRequestCount));
//                } else {
//                    Toast.makeText(this, "Permission not granted. App may not function correctly.", Toast.LENGTH_SHORT).show();
//                    Toast.makeText(this, "To show permission dialog, Extra > Fix CustTermux", Toast.LENGTH_SHORT).show();
//                }
//            } else {
//                // Permission already granted, reset the count and proceed with the app logic
//                preferenceManager.setKey("permissionRequestCount", "0");
//                proceedWithAppLogic();
//            }
//        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10 (API level 29)
            if (!Settings.canDrawOverlays(this)) {
                if (permissionRequestCount < 2) {
                    overapp_confirmation(this);
                    permissionRequestCount++;
                    preferenceManager.setKey("permissionRequestCount", String.valueOf(permissionRequestCount));
                } else {
                    Toast.makeText(this, "Draw Over Apps Permission not granted. App may not function correctly.", Toast.LENGTH_SHORT).show();
                    Toast.makeText(this, "To show permission dialog, Extra > Fix CustTermux", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Permission already granted, reset the count and proceed with the app logic
                preferenceManager.setKey("permissionRequestCount", "0");
                proceedWithAppLogic();
            }
        }

        ipAddressTextView = findViewById(R.id.ip_address);
        textplay = findViewById(R.id.textplay);

        String isLOCAL = preferenceManager.getKey("server_setup_isLocal");

        if (Objects.equals(isLOCAL, "Yes")) {
            Log.d("d", "Server is Local!");
            ipAddressTextView.setText("localhost");

            String ipport = preferenceManager.getKey("isLocalPORTonly");
            String a_playlink = "http://localhost:"+ipport+ "/playlist.m3u";
            String b_playlink = "Playlist url: "+a_playlink;
            textplay.setBackgroundResource(R.drawable.border);
            textplay.setText(b_playlink);
            preferenceManager.setKey("temp_playlist",a_playlink);
            //startFlashingEffect(textplay);

        } else {
            // Get and display Wi-Fi IP address
            String wifiIpAddress = getWifiIpAddress(this);
            preferenceManager.setKey("server_setup_wifiip",wifiIpAddress);
            ipAddressTextView.setText(wifiIpAddress);

            String ipport = preferenceManager.getKey("isLocalPORTonly");
            String a_playlink = "http://"+wifiIpAddress+":"+ipport+ "/playlist.m3u";
            String b_playlink = "Playlist url: "+a_playlink;
            textplay.setBackgroundResource(R.drawable.border);
            textplay.setText(b_playlink);
            preferenceManager.setKey("temp_playlist",a_playlink);
            //startFlashingEffect(textplay);
        }

        textplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startCOPY();
            }
        });









        String IPTVsetflag = preferenceManager.getKey("app_name");

        if (IPTVsetflag != null && !IPTVsetflag.equals("null") ) {
            iptvIcon.setVisibility(View.VISIBLE);
            String base64Image = preferenceManager.getKey("app_icon");
            if (base64Image != null && !base64Image.isEmpty()) {
                byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                iptvIcon.setImageBitmap(decodedBitmap);
            }
        } else {
            iptvIcon.setVisibility(View.GONE);
        }



        ipAddressTextView = findViewById(R.id.ip_address);
        downloadIcon = findViewById(R.id.ic_download);
//        copyIcon = findViewById(R.id.ic_copycat);
        
        downloadIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDownloadButtonClick();
            }
        });

        copyIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startCOPY();
            }
        });


        preferenceManager.setKey("isExit", "noExit");




//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Load termux shared preferences
        // This will also fail if TermuxConstants.TERMUX_PACKAGE_NAME does not equal applicationId
        mPreferences = TermuxAppSharedPreferences.build(this, true);
        if (mPreferences == null) {
            // An AlertDialog should have shown to kill the app, so we don't continue running activity code
            mIsInvalidState = true;
            return;
        }

        setMargins();

        mTermuxActivityRootView = findViewById(R.id.activity_termux_root_view);
        mTermuxActivityRootView.setActivity(this);
        mTermuxActivityBottomSpaceView = findViewById(R.id.activity_termux_bottom_space_view);
        mTermuxActivityRootView.setOnApplyWindowInsetsListener(new TermuxActivityRootView.WindowInsetsListener());

        View content = findViewById(android.R.id.content);
        content.setOnApplyWindowInsetsListener((v, insets) -> {
            mNavBarHeight = insets.getSystemWindowInsetBottom();
            return insets;
        });

        if (mProperties.isUsingFullScreen()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        setTermuxTerminalViewAndClients();

        setTerminalToolbarView(savedInstanceState);

        setSettingsButtonView();

        setNewSessionButtonView();

        setToggleKeyboardView();

        registerForContextMenu(mTerminalView);

        FileReceiverActivity.updateFileReceiverActivityComponentsState(this);

        try {
            // Start the {@link TermuxService} and make it run regardless of who is bound to it
            Intent serviceIntent = new Intent(this, TermuxService.class);
            startService(serviceIntent);

            // Attempt to bind to the service, this will call the {@link #onServiceConnected(ComponentName, IBinder)}
            // callback if it succeeds.
            if (!bindService(serviceIntent, this, 0))
                throw new RuntimeException("bindService() failed");
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "TermuxActivity failed to start TermuxService", e);
            Logger.showToast(this,
                getString(e.getMessage() != null && e.getMessage().contains("app is in background") ?
                    R.string.error_termux_service_start_failed_bg : R.string.error_termux_service_start_failed_general),
                true);
            mIsInvalidState = true;
            return;
        }

        // Send the {@link TermuxConstants#BROADCAST_TERMUX_OPENED} broadcast to notify apps that Termux
        // app has been opened.
        TermuxUtils.sendTermuxOpenedBroadcast(this);
    }
    ///////////////////////////////////////////////////////////////

//    // Example method to change media
//    public void changeMedia(String mediaUrl, String title) {
//        CastSession session = castHelper.getCastSession(); // Provide a method in CastHelper to get session if needed
//        castHelper.loadMedia(session, mediaUrl, title);
//    }


    private void startFlashingEffect(TextView textView) {
        ObjectAnimator animator = ObjectAnimator.ofInt(textView, "textColor", Color.WHITE, Color.RED);
        animator.setDuration(750);
        animator.setEvaluator(new android.animation.ArgbEvaluator());
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animator.setRepeatMode(ObjectAnimator.REVERSE);
        animator.start();
    }

    private void startIP() {
        SkySharedPref preferenceManager = new SkySharedPref(this);
        String isLOCAL = preferenceManager.getKey("server_setup_isLocal");

        if (Objects.equals(isLOCAL, "Yes")) {
            Log.d("d", "Server is Local!");
            ipAddressTextView.setText("localhost");

            String ipport = preferenceManager.getKey("isLocalPORTonly");
            String a_playlink = "http://localhost:"+ipport+ "/playlist.m3u";
            String b_playlink = "Playlist url: "+a_playlink;
            textplay.setBackgroundResource(R.drawable.border);
            textplay.setText(b_playlink);
            preferenceManager.setKey("temp_playlist",a_playlink);
            //startFlashingEffect(textplay);

        } else {
            // Get and display Wi-Fi IP address
            String wifiIpAddress = getWifiIpAddress(this);
            preferenceManager.setKey("server_setup_wifiip",wifiIpAddress);
            ipAddressTextView.setText(wifiIpAddress);

            String ipport = preferenceManager.getKey("isLocalPORTonly");

            if (Objects.equals(wifiIpAddress, "Error")){
                String x_playlink = "Network Error: "+ipport;
                textplay.setText(x_playlink);
                preferenceManager.setKey("temp_playlist",x_playlink);
            } else {
                String a_playlink = "http://"+wifiIpAddress+":"+ipport+ "/playlist.m3u";
                String b_playlink = "Playlist url: "+a_playlink;
                textplay.setText(b_playlink);
                preferenceManager.setKey("temp_playlist",a_playlink);
            }
            textplay.setBackgroundResource(R.drawable.border);

            //startFlashingEffect(textplay);
        }
    }



    private void startCOPY() {
        SkySharedPref preferenceManager = new SkySharedPref(TermuxActivity.this);
        copy_text = preferenceManager.getKey("temp_playlist");
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("playlist_link", copy_text);
        clipboard.setPrimaryClip(clip);

        // Show a toast message
        Toast.makeText(getApplicationContext(), "Playlist link copied to clipboard", Toast.LENGTH_SHORT).show();


    }


    private void sky_config() {
        Intent intent = new Intent(TermuxActivity.this, SetupActivityApp.class);
        startActivity(intent);
    }


    private void proceedWithAppLogic() {
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, REQUEST_CODE_MANAGE_OVERLAY_PERMISSION);
    }

//////////////////////////////////////////////////////////////



    private void lake_alert_confirmation(Context context) {
        // Create an AlertDialog Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.CustomAlertDialogTheme);

        // Set the message and the title
        builder.setMessage("Do you want to proceed?\n[Note: To exit press back button, reopen]")
            .setTitle("Confirmation");

        // Add the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                sky_terminal();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                dialog.dismiss();
            }
        });

        // Create the AlertDialog
        AlertDialog dialog = builder.create();

        // Show the AlertDialog
        dialog.show();
    }

    private void overapp_confirmation(Context context) {
        // Create an AlertDialog Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.CustomAlertDialogTheme);

        // Set the message and the title
        builder.setMessage("Draw over other apps permission required. To run server in background.")
            .setTitle("Permission Required");

        // Add the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestOverlayPermission();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                dialog.dismiss();
            }
        });

        // Create the AlertDialog
        AlertDialog dialog = builder.create();

        // Show the AlertDialog
        dialog.show();
    }


    private void coderun_alert_confirmation(Context context) {
        // Create an AlertDialog Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.CustomAlertDialogTheme);

        // Create an EditText view to get user input
        final EditText input = new EditText(context);
        input.setTextColor(context.getResources().getColor(R.color.text_color_black));
        input.setBackgroundTintList(ColorStateList.valueOf(000000));



        // Set the message and the title
        builder.setMessage("Enter code to run in terminal.")
            .setTitle("CodeRunner")
            .setView(input);  // Add the EditText to the dialog

        // Add the buttons
        builder.setPositiveButton("Send", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                String codeinput = input.getText().toString();
                Intent intentC = new Intent();
                intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
                intentC.setAction("com.termux.RUN_COMMAND");
                intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
                intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"runcode",codeinput});
                intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
                intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
                intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
                startService(intentC);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                dialog.dismiss();
            }
        });

        // Create the AlertDialog
        AlertDialog dialog = builder.create();

        // Show the AlertDialog
        dialog.show();
    }



    //////////////////////////////////////////////////////////////////////////////////////



    private void handleOverlayPermissionResult() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this);
        }
    }

    private void handleInstallPackagesPermissionResult() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkSelfPermission("android.permission.INSTALL_PACKAGES");

        }
    }


    public void wait_() {
        Handler handler1 = new Handler();
        Runnable runnable1 = new Runnable() {
            @Override
            public void run() {
                //EMPTY
            }
        };
        handler1.postDelayed(runnable1, 100);
    }



    private void XpkillIntent() {
        // Pkill Termux service
        Intent pkillIntent = new Intent();
        pkillIntent.setClassName("com.termux", "com.termux.app.RunCommandService");
        pkillIntent.setAction("com.termux.RUN_COMMAND");
        pkillIntent.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/pkill");
        pkillIntent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"-f","/data/data/com.termux/files/home/.jiotv_go/bin/jiotv_go"});
        pkillIntent.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        pkillIntent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
        pkillIntent.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        startService(pkillIntent);
        wait_();
    }

    private void XpkillIntentbg() {
        // Pkill Termux service
        Intent pkillIntent = new Intent();
        pkillIntent.setClassName("com.termux", "com.termux.app.RunCommandService");
        pkillIntent.setAction("com.termux.RUN_COMMAND");
        pkillIntent.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/pkill");
        pkillIntent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"-f","/data/data/com.termux/files/home/.jiotv_go/bin/jiotv_go"});
        pkillIntent.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        pkillIntent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true);
        pkillIntent.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        startService(pkillIntent);
        wait_();
    }

    private void XStopTermux() {
        // Stop the Termux service
        Intent termuxServiceStopIntent = new Intent();
        termuxServiceStopIntent.setClassName("com.termux", "com.termux.app.TermuxService");
        termuxServiceStopIntent.setAction("com.termux.service_stop");
        startService(termuxServiceStopIntent);
        //wait_();
    }

    private void XStartTermux() {
        // Start the Termux service
        Intent termuxServiceStartIntent = new Intent();
        termuxServiceStartIntent.setClassName("com.termux", "com.termux.app.TermuxService");
        termuxServiceStartIntent.setAction("com.termux.service_execute");
        startService(termuxServiceStartIntent);
        //wait_();
    }

    private void XStartTermuxAct() {
        // Start the Termux ACT service
        Intent IPTVIntent = new Intent();
        IPTVIntent.setClassName("com.Termux", "com.termux.app.TermuxActivity");
        startActivity(IPTVIntent);
        wait_();
    }

    private void XStartIPTV() {
        // Start the Start IPTV service
        Intent intentC = new Intent();
        intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
        intentC.setAction("com.termux.RUN_COMMAND");
        intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"iptvrunner"});
        intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true);
        intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        startService(intentC);
    }

    private void XStartEMPTY() {
        // Start the EMPTY
        Intent intentCz = new Intent();
        intentCz.setClassName("com.termux", "com.termux.app.RunCommandService");
        intentCz.setAction("com.termux.RUN_COMMAND");
        intentCz.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
        intentCz.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"iptvrunner2"});
        intentCz.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentCz.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
        intentCz.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        startService(intentCz);
    }

    public void launchTermux() {
        Intent intent = getPackageManager().getLaunchIntentForPackage("com.termux");
        if (intent != null) {
            //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // This flag is required if you're launching from outside an activity
            //startActivity(intent);
            Intent IPTVIntent = new Intent();
            IPTVIntent.setClassName("com.termux", "com.termux.app.TermuxActivity");
            startActivity(IPTVIntent);
        } else {
            // Termux app is not installed
            Toast.makeText(this, "Termux app is not installed.", Toast.LENGTH_SHORT).show();
        }
    }



    private int rerunCountx = 0; // Class-level variable to track the rerun count
    private static final int MAX_RERUN_COUNTx = 2; // Maximum number of times to rerun

    private void sky_rerun() {
        Toast.makeText(this, "Re-Running CustTermux", Toast.LENGTH_SHORT).show();
        XStartTermux();
        // Increment the rerun count
        rerunCountx++;

        // Check if the rerun count has reached the maximum limit
        if (rerunCountx >= MAX_RERUN_COUNTx) {
            LMN();
            rerunCountx = 0;
        }
    }

    private void LMN() {
        // Your logic for function LMN()
        Toast.makeText(this, "Restarting after 2 reruns", Toast.LENGTH_SHORT).show();
        Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (intent != null) {
            // Finish current activity
            finish();
            Log.d("SkyLog", "Out Of The App");
            // Restart the app
            startActivity(intent);

            // Exit the app
            System.exit(0);
        }
    }



    private void sky_exit() {
        // Finish current activity
        finish();

        // Exit the process
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);

        // Official killer
        XStopTermux();
        System.exit(0);
    }


    // Array to store media URLs and titles
    String[][] mediaList = {
        {"http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4", "THE CHROMA CASTER"},
        {"http://192.168.1.91/player/143", "THE M3U URL ip"},
        {"http://localhost:5001/player/143", "THE M3U URL local"},
        {"https://www.youtube.com/watch?v=QPLy0vHEXSA", "THE YT WAY"}
    };

    // Variable to track the current media index
    int currentMediaIndex = 0;

    private void sky_exit2() {
        // Increment the index to switch to the next media
        currentMediaIndex = (currentMediaIndex + 1) % mediaList.length;

        // Get the next media URL and title
        String newMediaUrl = mediaList[currentMediaIndex][0];
        String newTitle = mediaList[currentMediaIndex][1];

        Toast.makeText(TermuxActivity.this, "Playing" + newTitle, Toast.LENGTH_SHORT).show();

        // Load the new media
//        castHelper.loadMedia(castHelper.getCastSession(), newMediaUrl, newTitle);
    }



    private void sky_getter(){
        Intent intent = new Intent();
        intent.setAction("com.termux.GetReceiver");
        intent.setComponent(new ComponentName("com.termux", "com.termux.SkySharedPrefActivity"));
        intent.putExtra("key", "exampleKey");
        startActivity(intent);

        // Register a receiver to get the response
        BroadcastReceiver responseReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String key = intent.getStringExtra("key");
                String value = intent.getStringExtra("value");
                // Use the key and value as needed
                Toast.makeText(TermuxActivity.this, "Server not available."+key+"---"+value, Toast.LENGTH_SHORT).show();
            }
        };

        IntentFilter filter = new IntentFilter("com.termux.GetResponse");
        registerReceiver(responseReceiver, filter);
    }

    private void sky_saver(){
        Intent intent = new Intent();
        intent.setAction("com.termux.SaveReceiver");
        intent.setComponent(new ComponentName("com.termux", "com.termux.SkySharedPrefActivity"));
        intent.putExtra("key", "exampleKey");
        intent.putExtra("value", "exampleValue2");
        startActivity(intent);
    }


    public void sky_terminal() {
        TerminalView terminalView = findViewById(R.id.terminal_view);

        // Change focusable properties
        terminalView.setFocusableInTouchMode(true);
        terminalView.setFocusable(true);
    }



    private void sky_tv() {
        Intent intent = new Intent();
        intent.setAction("com.termux.SkyTV");
        intent.putExtra("call", "start");
        intent.setPackage("com.termux");
        startActivity(intent);
    }

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(this, "Storage permission is required. Please allow this permission in App Settings.", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }


    private void downloadFile(String fileUrl, String extraString) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(fileUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.connect();

                    InputStream inputStream = new BufferedInputStream(connection.getInputStream());

                    // Define the file name with the extra string
                    String fileName = "playlist";
                    if (extraString != null && !extraString.isEmpty()) {
                        fileName += "_" + extraString;
                    }
                    SkySharedPref preferenceManager = new SkySharedPref(TermuxActivity.this);
                    ipport = preferenceManager.getKey("isLocalPORTonly");
                    fileName += "_" + ipport;
                    fileName += ".m3u";


                    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
                    FileOutputStream outputStream = new FileOutputStream(file);

                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }

                    outputStream.close();
                    inputStream.close();

                    // Update UI on the main thread
                    String finalFileName = fileName;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(TermuxActivity.this, "Playlist downloaded to Downloads folder as " + finalFileName, Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    // Update UI on the main thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(TermuxActivity.this, "Download failed", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    public void onDownloadButtonClick() {
        if (checkPermission()) {
            SkySharedPref preferenceManager = new SkySharedPref(this);
            String isLocal = preferenceManager.getKey("server_setup_isLocal");

            // Define the listeners
            DialogInterface.OnClickListener localListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    urlport = preferenceManager.getKey("isLocalPORT");
                    downloadUrl = urlport+"playlist.m3u";
                    downloadFile(downloadUrl, "local");
                }
            };

            DialogInterface.OnClickListener publicListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String wifiIpAddress = getWifiIpAddress();
                    SkySharedPref preferenceManager = new SkySharedPref(TermuxActivity.this);
                    urlportonly = preferenceManager.getKey("isLocalPORTonly");
                    downloadUrl = "http://" + wifiIpAddress + ":"+urlportonly+"/playlist.m3u";
                    downloadFile(downloadUrl, wifiIpAddress);
                }
            };

            if (isLocal != null && !isLocal.isEmpty()) {
                if (isLocal.equals("Yes")) {
                    urlport = preferenceManager.getKey("isLocalPORT");
                    downloadUrl = urlport+"playlist.m3u";
                    downloadFile(downloadUrl, "local");
                } else {
                    Utils.showAlertbox_playlist(this, localListener, publicListener);
                }
            } else {
                Utils.showAlertbox_playlist(this, localListener, publicListener);
            }
        } else {
            requestPermission();
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////



//    private void clearAppData() {
//        try {
//            // Clearing app data
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//                boolean success = ((ActivityManager) getSystemService(ACTIVITY_SERVICE)).clearApplicationUserData();
//                Log.d("ClearAppData", "Clear user data success: " + success);
//            } else {
//                String packageName = getApplicationContext().getPackageName();
//                Runtime runtime = Runtime.getRuntime();
//                runtime.exec("pm clear " + packageName);
//                Log.d("ClearAppData", "App data cleared via pm clear for package: " + packageName);
//            }
//
//            // Restart the app
//            Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
//            if (intent != null) {
//                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                startActivity(intent);
//                finish(); // Optional: finish the current activity
//            } else {
//                Log.e("ClearAppData", "Failed to get launch intent for package: " + getPackageName());
//            }
//        } catch (Exception e) {
//            Log.e("ClearAppData", "Failed to clear app data", e);
//        }
//    }

    private void clearAppData() {
        try {
            // Clearing app data for SDK version KITKAT and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);

                if (activityManager != null) {
                    boolean success = activityManager.clearApplicationUserData();
                    Log.d("ClearAppData", "Clear user data success: " + success);
                } else {
                    Log.e("ClearAppData", "ActivityManager is null, unable to clear data.");
                }

            } else {
                // Clearing app data via pm clear command for older Android versions
                String packageName = getApplicationContext().getPackageName();
                try {
                    Runtime.getRuntime().exec("pm clear " + packageName);
                    Log.d("ClearAppData", "App data cleared via pm clear for package: " + packageName);
                } catch (IOException e) {
                    Log.e("ClearAppData", "Failed to execute pm clear command", e);
                }
            }

            // Restart the app
            restartApp();

        } catch (Exception e) {
            Log.e("ClearAppData", "Failed to clear app data", e);
        }
    }

    private void restartApp() {
        try {
            // Get the launch intent for restarting the app
            Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());

            if (intent != null) {
                // Clear all activities and restart the app from the root activity
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finishAffinity(); // Ensure all activities are finished
            } else {
                Log.e("ClearAppData", "Failed to get launch intent for package: " + getPackageName());
            }
        } catch (Exception e) {
            Log.e("ClearAppData", "Failed to restart the app", e);
        }
    }



    @Override
    public void onStart() {
        super.onStart();
        SkySharedPref preferenceManager = new SkySharedPref(this);
        String serverSetupDone = preferenceManager.getKey("isServerSetupDone");

        if (serverSetupDone != null && serverSetupDone.equals("Done")) {
            //sky_exit();
        } else {
            preferenceManager.setKey("tag_name", "0.121x");

            preferenceManager.setKey("isLocalNOPORT", "http://localhost:");
            preferenceManager.setKey("isLocalPORT", "http://localhost:5001/");
            preferenceManager.setKey("isLocalPORTchannel", "live/144.m3u8");
            preferenceManager.setKey("isLocalPORTonly", "5001");
            preferenceManager.setKey("server_setup_isLoginCheck", "Yes");
            preferenceManager.setKey("server_setup_isAutoboot", "No");
            preferenceManager.setKey("server_setup_isAutobootBG", "No");
            preferenceManager.setKey("server_setup_isLocal", "No");
            preferenceManager.setKey("app_name", "null");
            preferenceManager.setKey("app_launchactivity", "null");
            preferenceManager.setKey("isExit", "noExit");
            preferenceManager.setKey("server_setup_isEPG", "Yes");
            preferenceManager.setKey("server_setup_isGenericBanner", "No");
            preferenceManager.setKey("server_setup_isSSH", "No");
            preferenceManager.setKey("isDelayTime", "5");
            preferenceManager.setKey("permissionRequestCount", "0");
            preferenceManager.setKey("isFlagSetForMinimize", "No");
            preferenceManager.setKey("isWEBTVconfig", "");
            preferenceManager.setKey("versionCustScript", BuildConfig.VERSION_CUST_SCRIPT);

            Utils.changeIconTOFirst(this);

            Intent intent = new Intent(TermuxActivity.this, SetupActivity.class);
            startActivity(intent);
        }
//        Toast.makeText(TermuxActivity.this, "Don't make us famous", Toast.LENGTH_SHORT).show();


        String custVar = BuildConfig.VERSION_CUST_SCRIPT; //4.4
        String versionCustScript = preferenceManager.getKey("versionCustScript"); //null or 4.3


        //preferenceManager.setKey("versionCustScript", "4.3");
        if (!custVar.equals(versionCustScript)) {
            Utils.showAlertbox(this, "CTx Reset Required",
                "To function properly, please clear the app's data.",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        preferenceManager.setKey("versionCustScript", custVar);
                        Toast.makeText(TermuxActivity.this, "Clearing App Data", Toast.LENGTH_SHORT).show();
                        clearAppData();
                    }
                });
        }





        Logger.logDebug(LOG_TAG, "onStart");

        //System.out.println("Error occurred while checking status code.");
        if (mIsInvalidState) return;

        mIsVisible = true;

        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.onStart();

        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onStart();

        if (mPreferences.isTerminalMarginAdjustmentEnabled())
            addTermuxActivityRootViewGlobalLayoutListener();


        registerTermuxActivityBroadcastReceiver();
    }




    public void onPause() {
        super.onPause();
        termuxActivityResume.onPause();

        serverStatusChecker.stopChecking();
        loginStatusChecker.stopChecking();

//        castHelper.removeSessionManagerListener();
    }



    private int openIptvCount = 0;
    private final int maxOpenIptvCalls = 10;



    @Override
    public void onResume() {
        super.onResume();
        SkySharedPref preferenceManager = new SkySharedPref(this);
        String serverSetupDone = preferenceManager.getKey("isServerSetupDone");
        if (serverSetupDone != null && serverSetupDone.equals("Done")) {
            //sky_exit();
        } else {
            Intent intent = new Intent(TermuxActivity.this, SetupActivity.class);
            startActivity(intent);
        }

        Logger.logVerbose(LOG_TAG, "onResume");

        if (mIsInvalidState) return;

        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.onResume();

        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onResume();

        // Check if a crash happened on last run of the app or if a plugin crashed and show a
        // notification with the crash details if it did
        TermuxCrashUtils.notifyAppCrashFromCrashLogFile(this, LOG_TAG);

        mIsOnResumeAfterOnCreate = false;

        Button button1 = findViewById(R.id.button1);
        button1.requestFocus();

        termuxActivityResume.onResume();

        serverStatusChecker.startChecking();
        loginStatusChecker.startChecking();

        startIP();

//        castHelper.addSessionManagerListener();
    }




//    public String getWifiIpAddress(Context context) {
//        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
//        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
//        int ipAddress = wifiInfo.getIpAddress();
//        byte[] ipAddressBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipAddress).array();
//
//        try {
//            InetAddress inetAddress = InetAddress.getByAddress(ipAddressBytes);
//            String ipAddressStr = inetAddress.getHostAddress();
//
//            SkySharedPref preferenceManager = new SkySharedPref(this);
//            preferenceManager.setKey("wifi1", ipAddressStr);
//
//            return ipAddressStr;
//        } catch (UnknownHostException e) {
//            Log.e("WifiIP", "Failed to get host address", e);
//            return null;
//        }
//    }

    public String getWifiIpAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        if (wifiInfo != null && wifiInfo.getNetworkId() != -1) {
            // Try getting Wi-Fi IP address
            int wifiIpAddress = wifiInfo.getIpAddress();
            byte[] wifiIpAddressBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(wifiIpAddress).array();

            try {
                InetAddress inetAddress = InetAddress.getByAddress(wifiIpAddressBytes);
                String wifiIpAddressStr = inetAddress.getHostAddress();

                SkySharedPref preferenceManager = new SkySharedPref(context);
                preferenceManager.setKey("wifi1", wifiIpAddressStr);

                return wifiIpAddressStr;
            } catch (UnknownHostException e) {
                Log.e("IP", "Failed to get Wi-Fi host address", e);
            }
        }

        // If Wi-Fi is not connected, try Ethernet
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                if (!networkInterface.isLoopback() && networkInterface.isUp() &&
                    (networkInterface.getName().contains("eth") || networkInterface.getName().contains("wlan"))) {

                    Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress inetAddress = addresses.nextElement();
                        if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                            String ethernetIpAddressStr = inetAddress.getHostAddress();

                            SkySharedPref preferenceManager = new SkySharedPref(context);
                            preferenceManager.setKey("ethernet1", ethernetIpAddressStr);

                            return ethernetIpAddressStr;
                        }
                    }
                }
            }
        } catch (SocketException e) {
            Log.e("IP", "Failed to get Ethernet address", e);
        }

        // If neither Wi-Fi nor Ethernet is connected
        return "Error";
    }


    public String getWifiIpAddress() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            return "Wi-Fi Manager is null";
        }

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo == null) {
            return "Wi-Fi Info is null";
        }

        int ipAddress = wifiInfo.getIpAddress();
        byte[] ipAddressBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipAddress).array();

        try {
            InetAddress inetAddress = InetAddress.getByAddress(ipAddressBytes);
            String ipAddressStr = inetAddress.getHostAddress();

            // Save the IP address in shared preferences
            SkySharedPref preferenceManager = new SkySharedPref(this);
            preferenceManager.setKey("wifi2", ipAddressStr);

            return ipAddressStr;
        } catch (UnknownHostException e) {
            Log.e("WifiIP", "Failed to get host address", e);
            return "Unknown host exception";
        }
    }








    @Override
    protected void onStop() {
        super.onStop();

        Logger.logDebug(LOG_TAG, "onStop");

        if (mIsInvalidState) return;

        mIsVisible = false;

        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.onStop();

        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onStop();

        removeTermuxActivityRootViewGlobalLayoutListener();

        unregisterTermuxActivityBroadcastReceiver();
        getDrawer().closeDrawers();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && autoDismissRunnable != null) {
            handler.removeCallbacks(autoDismissRunnable);
        }
        openIptvCount = 0;

        if (serverStatusChecker != null) {
            serverStatusChecker.stopChecking();
        }

        Logger.logDebug(LOG_TAG, "onDestroy");

        if (mIsInvalidState) return;

        if (mTermuxService != null) {
            // Do not leave service and session clients with references to activity.
            mTermuxService.unsetTermuxTerminalSessionClient();
            mTermuxService = null;
        }

        try {
            unbindService(this);
        } catch (Exception e) {
            // ignore.
        }

    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        Logger.logVerbose(LOG_TAG, "onSaveInstanceState");

        super.onSaveInstanceState(savedInstanceState);
        saveTerminalToolbarTextInput(savedInstanceState);
        savedInstanceState.putBoolean(ARG_ACTIVITY_RECREATED, true);
    }





    /**
     * Part of the {@link ServiceConnection} interface. The service is bound with
     * {@link #bindService(Intent, ServiceConnection, int)} in {@link #onCreate(Bundle)} which will cause a call to this
     * callback method.
     */
    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
        Logger.logDebug(LOG_TAG, "onServiceConnected");

        mTermuxService = ((TermuxService.LocalBinder) service).service;

        setTermuxSessionsListView();

        final Intent intent = getIntent();
        setIntent(null);

        if (mTermuxService.isTermuxSessionsEmpty()) {
            if (mIsVisible) {
                TermuxInstaller.setupBootstrapIfNeeded(TermuxActivity.this, () -> {
                    if (mTermuxService == null) return; // Activity might have been destroyed.
                    try {
                        boolean launchFailsafe = false;
                        if (intent != null && intent.getExtras() != null) {
                            launchFailsafe = intent.getExtras().getBoolean(TERMUX_ACTIVITY.EXTRA_FAILSAFE_SESSION, false);
                        }
                        mTermuxTerminalSessionActivityClient.addNewSession(launchFailsafe, null);
                    } catch (WindowManager.BadTokenException e) {
                        // Activity finished - ignore.
                    }
                });
            } else {
                // The service connected while not in foreground - just bail out.
                finishActivityIfNotFinishing();
            }
        } else {
            // If termux was started from launcher "New session" shortcut and activity is recreated,
            // then the original intent will be re-delivered, resulting in a new session being re-added
            // each time.
            if (!mIsActivityRecreated && intent != null && Intent.ACTION_RUN.equals(intent.getAction())) {
                // Android 7.1 app shortcut from res/xml/shortcuts.xml.
                boolean isFailSafe = intent.getBooleanExtra(TERMUX_ACTIVITY.EXTRA_FAILSAFE_SESSION, false);
                mTermuxTerminalSessionActivityClient.addNewSession(isFailSafe, null);
            } else {
                mTermuxTerminalSessionActivityClient.setCurrentSession(mTermuxTerminalSessionActivityClient.getCurrentStoredSessionOrLast());
            }
        }

        // Update the {@link TerminalSession} and {@link TerminalEmulator} clients.
        mTermuxService.setTermuxTerminalSessionClient(mTermuxTerminalSessionActivityClient);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Logger.logDebug(LOG_TAG, "onServiceDisconnected");

        // Respect being stopped from the {@link TermuxService} notification action.
        finishActivityIfNotFinishing();
    }






    private void reloadProperties() {
        mProperties.loadTermuxPropertiesFromDisk();

        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onReloadProperties();
    }



    private void setActivityTheme() {
        // Update NightMode.APP_NIGHT_MODE
        TermuxThemeUtils.setAppNightMode(mProperties.getNightMode());

        // Set activity night mode. If NightMode.SYSTEM is set, then android will automatically
        // trigger recreation of activity when uiMode/dark mode configuration is changed so that
        // day or night theme takes affect.
        AppCompatActivityUtils.setNightMode(this, NightMode.getAppNightMode().getName(), true);
    }

    private void setMargins() {
        RelativeLayout relativeLayout = findViewById(R.id.activity_termux_root_relative_layout);
        int marginHorizontal = mProperties.getTerminalMarginHorizontal();
        int marginVertical = mProperties.getTerminalMarginVertical();
        ViewUtils.setLayoutMarginsInDp(relativeLayout, marginHorizontal, marginVertical, marginHorizontal, marginVertical);
    }



    public void addTermuxActivityRootViewGlobalLayoutListener() {
        getTermuxActivityRootView().getViewTreeObserver().addOnGlobalLayoutListener(getTermuxActivityRootView());
    }

    public void removeTermuxActivityRootViewGlobalLayoutListener() {
        if (getTermuxActivityRootView() != null)
            getTermuxActivityRootView().getViewTreeObserver().removeOnGlobalLayoutListener(getTermuxActivityRootView());
    }



    private void setTermuxTerminalViewAndClients() {
        // Set termux terminal view and session clients
        mTermuxTerminalSessionActivityClient = new TermuxTerminalSessionActivityClient(this);
        mTermuxTerminalViewClient = new TermuxTerminalViewClient(this, mTermuxTerminalSessionActivityClient);

        // Set termux terminal view
        mTerminalView = findViewById(R.id.terminal_view);



        mTerminalView.setTerminalViewClient(mTermuxTerminalViewClient);

        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onCreate();

        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.onCreate();
    }

    private void setTermuxSessionsListView() {
        ListView termuxSessionsListView = findViewById(R.id.terminal_sessions_list);
        mTermuxSessionListViewController = new TermuxSessionsListViewController(this, mTermuxService.getTermuxSessions());
        termuxSessionsListView.setAdapter(mTermuxSessionListViewController);
        termuxSessionsListView.setOnItemClickListener(mTermuxSessionListViewController);
        termuxSessionsListView.setOnItemLongClickListener(mTermuxSessionListViewController);
    }



    private void setTerminalToolbarView(Bundle savedInstanceState) {
        mTermuxTerminalExtraKeys = new TermuxTerminalExtraKeys(this, mTerminalView,
            mTermuxTerminalViewClient, mTermuxTerminalSessionActivityClient);

        final ViewPager terminalToolbarViewPager = getTerminalToolbarViewPager();
        if (mPreferences.shouldShowTerminalToolbar()) terminalToolbarViewPager.setVisibility(View.VISIBLE);

        ViewGroup.LayoutParams layoutParams = terminalToolbarViewPager.getLayoutParams();
        mTerminalToolbarDefaultHeight = layoutParams.height;

        setTerminalToolbarHeight();

        String savedTextInput = null;
        if (savedInstanceState != null)
            savedTextInput = savedInstanceState.getString(ARG_TERMINAL_TOOLBAR_TEXT_INPUT);

        terminalToolbarViewPager.setAdapter(new TerminalToolbarViewPager.PageAdapter(this, savedTextInput));
        terminalToolbarViewPager.addOnPageChangeListener(new TerminalToolbarViewPager.OnPageChangeListener(this, terminalToolbarViewPager));
    }

    private void setTerminalToolbarHeight() {
        final ViewPager terminalToolbarViewPager = getTerminalToolbarViewPager();
        if (terminalToolbarViewPager == null) return;

        ViewGroup.LayoutParams layoutParams = terminalToolbarViewPager.getLayoutParams();
        layoutParams.height = Math.round(mTerminalToolbarDefaultHeight *
            (mTermuxTerminalExtraKeys.getExtraKeysInfo() == null ? 0 : mTermuxTerminalExtraKeys.getExtraKeysInfo().getMatrix().length) *
            mProperties.getTerminalToolbarHeightScaleFactor());
        terminalToolbarViewPager.setLayoutParams(layoutParams);
    }

    public void toggleTerminalToolbar() {
        final ViewPager terminalToolbarViewPager = getTerminalToolbarViewPager();
        if (terminalToolbarViewPager == null) return;

        final boolean showNow = mPreferences.toogleShowTerminalToolbar();
        Logger.showToast(this, (showNow ? getString(R.string.msg_enabling_terminal_toolbar) : getString(R.string.msg_disabling_terminal_toolbar)), true);
        terminalToolbarViewPager.setVisibility(showNow ? View.VISIBLE : View.GONE);
        if (showNow && isTerminalToolbarTextInputViewSelected()) {
            // Focus the text input view if just revealed.
            findViewById(R.id.terminal_toolbar_text_input).requestFocus();
        }
    }

    private void saveTerminalToolbarTextInput(Bundle savedInstanceState) {
        if (savedInstanceState == null) return;

        final EditText textInputView = findViewById(R.id.terminal_toolbar_text_input);
        if (textInputView != null) {
            String textInput = textInputView.getText().toString();
            if (!textInput.isEmpty()) savedInstanceState.putString(ARG_TERMINAL_TOOLBAR_TEXT_INPUT, textInput);
        }
    }



    private void setSettingsButtonView() {
        ImageButton settingsButton = findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(v -> {
            ActivityUtils.startActivity(this, new Intent(this, SettingsActivity.class));
        });
    }

    private void setNewSessionButtonView() {
        View newSessionButton = findViewById(R.id.new_session_button);
        newSessionButton.setOnClickListener(v -> mTermuxTerminalSessionActivityClient.addNewSession(false, null));
        newSessionButton.setOnLongClickListener(v -> {
            TextInputDialogUtils.textInput(TermuxActivity.this, R.string.title_create_named_session, null,
                R.string.action_create_named_session_confirm, text -> mTermuxTerminalSessionActivityClient.addNewSession(false, text),
                R.string.action_new_session_failsafe, text -> mTermuxTerminalSessionActivityClient.addNewSession(true, text),
                -1, null, null);
            return true;
        });
    }

    private void setToggleKeyboardView() {
        findViewById(R.id.toggle_keyboard_button).setOnClickListener(v -> {
            mTermuxTerminalViewClient.onToggleSoftKeyboardRequest();
            getDrawer().closeDrawers();
        });

        findViewById(R.id.toggle_keyboard_button).setOnLongClickListener(v -> {
            toggleTerminalToolbar();
            return true;
        });
    }





    @SuppressLint("RtlHardcoded")
    @Override
    public void onBackPressed() {
        if (getDrawer().isDrawerOpen(Gravity.LEFT)) {
            getDrawer().closeDrawers();
        } else {
            finishActivityIfNotFinishing();
        }
    }

    public void finishActivityIfNotFinishing() {
        // prevent duplicate calls to finish() if called from multiple places
        if (!TermuxActivity.this.isFinishing()) {
            finish();
        }
    }

    /** Show a toast and dismiss the last one if still visible. */
    public void showToast(String text, boolean longDuration) {
        if (text == null || text.isEmpty()) return;
        if (mLastToast != null) mLastToast.cancel();
        mLastToast = Toast.makeText(TermuxActivity.this, text, longDuration ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        mLastToast.setGravity(Gravity.TOP, 0, 0);
        mLastToast.show();
    }



    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        TerminalSession currentSession = getCurrentSession();
        if (currentSession == null) return;

        boolean addAutoFillMenu = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AutofillManager autofillManager = getSystemService(AutofillManager.class);
            if (autofillManager != null && autofillManager.isEnabled()) {
                addAutoFillMenu = true;
            }
        }

        menu.add(Menu.NONE, CONTEXT_MENU_SELECT_URL_ID, Menu.NONE, R.string.action_select_url);
        menu.add(Menu.NONE, CONTEXT_MENU_SHARE_TRANSCRIPT_ID, Menu.NONE, R.string.action_share_transcript);
        if (!DataUtils.isNullOrEmpty(mTerminalView.getStoredSelectedText()))
            menu.add(Menu.NONE, CONTEXT_MENU_SHARE_SELECTED_TEXT, Menu.NONE, R.string.action_share_selected_text);
        if (addAutoFillMenu)
            menu.add(Menu.NONE, CONTEXT_MENU_AUTOFILL_ID, Menu.NONE, R.string.action_autofill_password);
        menu.add(Menu.NONE, CONTEXT_MENU_RESET_TERMINAL_ID, Menu.NONE, R.string.action_reset_terminal);
        menu.add(Menu.NONE, CONTEXT_MENU_KILL_PROCESS_ID, Menu.NONE, getResources().getString(R.string.action_kill_process, getCurrentSession().getPid())).setEnabled(currentSession.isRunning());
        menu.add(Menu.NONE, CONTEXT_MENU_STYLING_ID, Menu.NONE, R.string.action_style_terminal);
        menu.add(Menu.NONE, CONTEXT_MENU_TOGGLE_KEEP_SCREEN_ON, Menu.NONE, R.string.action_toggle_keep_screen_on).setCheckable(true).setChecked(mPreferences.shouldKeepScreenOn());
        menu.add(Menu.NONE, CONTEXT_MENU_HELP_ID, Menu.NONE, R.string.action_open_help);
        menu.add(Menu.NONE, CONTEXT_MENU_SETTINGS_ID, Menu.NONE, R.string.action_open_settings);
        menu.add(Menu.NONE, CONTEXT_MENU_REPORT_ID, Menu.NONE, R.string.action_report_issue);
    }

    /** Hook system menu to show context menu instead. */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mTerminalView.showContextMenu();
        return false;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        TerminalSession session = getCurrentSession();

        switch (item.getItemId()) {
            case CONTEXT_MENU_SELECT_URL_ID:
                mTermuxTerminalViewClient.showUrlSelection();
                return true;
            case CONTEXT_MENU_SHARE_TRANSCRIPT_ID:
                mTermuxTerminalViewClient.shareSessionTranscript();
                return true;
            case CONTEXT_MENU_SHARE_SELECTED_TEXT:
                mTermuxTerminalViewClient.shareSelectedText();
                return true;
            case CONTEXT_MENU_AUTOFILL_ID:
                requestAutoFill();
                return true;
            case CONTEXT_MENU_RESET_TERMINAL_ID:
                onResetTerminalSession(session);
                return true;
            case CONTEXT_MENU_KILL_PROCESS_ID:
                showKillSessionDialog(session);
                return true;
            case CONTEXT_MENU_STYLING_ID:
                showStylingDialog();
                return true;
            case CONTEXT_MENU_TOGGLE_KEEP_SCREEN_ON:
                toggleKeepScreenOn();
                return true;
            case CONTEXT_MENU_HELP_ID:
                ActivityUtils.startActivity(this, new Intent(this, HelpActivity.class));
                return true;
            case CONTEXT_MENU_SETTINGS_ID:
                ActivityUtils.startActivity(this, new Intent(this, SettingsActivity.class));
                return true;
            case CONTEXT_MENU_REPORT_ID:
                mTermuxTerminalViewClient.reportIssueFromTranscript();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onContextMenuClosed(Menu menu) {
        super.onContextMenuClosed(menu);
        // onContextMenuClosed() is triggered twice if back button is pressed to dismiss instead of tap for some reason
        mTerminalView.onContextMenuClosed(menu);
    }

    private void showKillSessionDialog(TerminalSession session) {
        if (session == null) return;

        final AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setIcon(android.R.drawable.ic_dialog_alert);
        b.setMessage(R.string.title_confirm_kill_process);
        b.setPositiveButton(android.R.string.yes, (dialog, id) -> {
            dialog.dismiss();
            session.finishIfRunning();
        });
        b.setNegativeButton(android.R.string.no, null);
        b.show();
    }

    private void onResetTerminalSession(TerminalSession session) {
        if (session != null) {
            session.reset();
            showToast(getResources().getString(R.string.msg_terminal_reset), true);

            if (mTermuxTerminalSessionActivityClient != null)
                mTermuxTerminalSessionActivityClient.onResetTerminalSession();
        }
    }

    private void showStylingDialog() {
        Intent stylingIntent = new Intent();
        stylingIntent.setClassName(TermuxConstants.TERMUX_STYLING_PACKAGE_NAME, TermuxConstants.TERMUX_STYLING.TERMUX_STYLING_ACTIVITY_NAME);
        try {
            startActivity(stylingIntent);
        } catch (ActivityNotFoundException | IllegalArgumentException e) {
            // The startActivity() call is not documented to throw IllegalArgumentException.
            // However, crash reporting shows that it sometimes does, so catch it here.
            new AlertDialog.Builder(this).setMessage(getString(R.string.error_styling_not_installed))
                .setPositiveButton(R.string.action_styling_install,
                    (dialog, which) -> ActivityUtils.startActivity(this, new Intent(Intent.ACTION_VIEW, Uri.parse(TermuxConstants.TERMUX_STYLING_FDROID_PACKAGE_URL))))
                .setNegativeButton(android.R.string.cancel, null).show();
        }
    }
    private void toggleKeepScreenOn() {
        if (mTerminalView.getKeepScreenOn()) {
            mTerminalView.setKeepScreenOn(false);
            mPreferences.setKeepScreenOn(false);
        } else {
            mTerminalView.setKeepScreenOn(true);
            mPreferences.setKeepScreenOn(true);
        }
    }

    private void requestAutoFill() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AutofillManager autofillManager = getSystemService(AutofillManager.class);
            if (autofillManager != null && autofillManager.isEnabled()) {
                autofillManager.requestAutofill(mTerminalView);
            }
        }
    }



    /**
     * For processes to access primary external storage (/sdcard, /storage/emulated/0, ~/storage/shared),
     * termux needs to be granted legacy WRITE_EXTERNAL_STORAGE or MANAGE_EXTERNAL_STORAGE permissions
     * if targeting targetSdkVersion 30 (android 11) and running on sdk 30 (android 11) and higher.
     */
    public void requestStoragePermission(boolean isPermissionCallback) {
        new Thread() {
            @Override
            public void run() {
                // Do not ask for permission again
                int requestCode = isPermissionCallback ? -1 : PermissionUtils.REQUEST_GRANT_STORAGE_PERMISSION;

                // If permission is granted, then also setup storage symlinks.
                if(PermissionUtils.checkAndRequestLegacyOrManageExternalStoragePermission(
                    TermuxActivity.this, requestCode, true, !isPermissionCallback)) {
                    if (isPermissionCallback)
                        Logger.logInfoAndShowToast(TermuxActivity.this, LOG_TAG,
                            getString(com.termux.shared.R.string.msg_storage_permission_granted_on_request));

                    TermuxInstaller.setupStorageSymlinks(TermuxActivity.this);
                } else {
                    if (isPermissionCallback)
                        Logger.logInfoAndShowToast(TermuxActivity.this, LOG_TAG,
                            getString(com.termux.shared.R.string.msg_storage_permission_not_granted_on_request));
                }
            }
        }.start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Logger.logVerbose(LOG_TAG, "onActivityResult: requestCode: " + requestCode + ", resultCode: "  + resultCode + ", data: "  + IntentUtils.getIntentString(data));
        //if (requestCode == PermissionUtils.REQUEST_GRANT_STORAGE_PERMISSION) {
        //    requestStoragePermission(true);
        //}
        //handleOverlayPermissionResult(requestCode);
        //super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_MANAGE_OVERLAY_PERMISSION) {
            handleOverlayPermissionResult();
        } else if (requestCode == REQUEST_CODE_INSTALL_PACKAGES_PERMISSION) {
            handleInstallPackagesPermissionResult();}
        else if (requestCode == PermissionUtils.REQUEST_GRANT_STORAGE_PERMISSION) {
            requestStoragePermission(true);
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Logger.logVerbose(LOG_TAG, "onRequestPermissionsResult: requestCode: " + requestCode + ", permissions: "  + Arrays.toString(permissions) + ", grantResults: "  + Arrays.toString(grantResults));
        if (requestCode == PermissionUtils.REQUEST_GRANT_STORAGE_PERMISSION) {
            requestStoragePermission(true);
        }
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onDownloadButtonClick();
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }




    public int getNavBarHeight() {
        return mNavBarHeight;
    }

    public TermuxActivityRootView getTermuxActivityRootView() {
        return mTermuxActivityRootView;
    }

    public View getTermuxActivityBottomSpaceView() {
        return mTermuxActivityBottomSpaceView;
    }

    public ExtraKeysView getExtraKeysView() {
        return mExtraKeysView;
    }

    public TermuxTerminalExtraKeys getTermuxTerminalExtraKeys() {
        return mTermuxTerminalExtraKeys;
    }

    public void setExtraKeysView(ExtraKeysView extraKeysView) {
        mExtraKeysView = extraKeysView;
    }

    public DrawerLayout getDrawer() {
        return (DrawerLayout) findViewById(R.id.drawer_layout);
    }


    public ViewPager getTerminalToolbarViewPager() {
        return (ViewPager) findViewById(R.id.terminal_toolbar_view_pager);
    }

    public float getTerminalToolbarDefaultHeight() {
        return mTerminalToolbarDefaultHeight;
    }

    public boolean isTerminalViewSelected() {
        return getTerminalToolbarViewPager().getCurrentItem() == 0;
    }

    public boolean isTerminalToolbarTextInputViewSelected() {
        return getTerminalToolbarViewPager().getCurrentItem() == 1;
    }


    public void termuxSessionListNotifyUpdated() {
        mTermuxSessionListViewController.notifyDataSetChanged();
    }

    public boolean isVisible() {
        return mIsVisible;
    }

    public boolean isOnResumeAfterOnCreate() {
        return mIsOnResumeAfterOnCreate;
    }

    public boolean isActivityRecreated() {
        return mIsActivityRecreated;
    }



    public TermuxService getTermuxService() {
        return mTermuxService;
    }

    public TerminalView getTerminalView() {
        return mTerminalView;
    }

    public TermuxTerminalViewClient getTermuxTerminalViewClient() {
        return mTermuxTerminalViewClient;
    }

    public TermuxTerminalSessionActivityClient getTermuxTerminalSessionClient() {
        return mTermuxTerminalSessionActivityClient;
    }

    @Nullable
    public TerminalSession getCurrentSession() {
        if (mTerminalView != null)
            return mTerminalView.getCurrentSession();
        else
            return null;
    }

    public TermuxAppSharedPreferences getPreferences() {
        return mPreferences;
    }

    public TermuxAppSharedProperties getProperties() {
        return mProperties;
    }




    public static void updateTermuxActivityStyling(Context context, boolean recreateActivity) {
        // Make sure that terminal styling is always applied.
        Intent stylingIntent = new Intent(TERMUX_ACTIVITY.ACTION_RELOAD_STYLE);
        stylingIntent.putExtra(TERMUX_ACTIVITY.EXTRA_RECREATE_ACTIVITY, recreateActivity);
        context.sendBroadcast(stylingIntent);
    }

    private void registerTermuxActivityBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TERMUX_ACTIVITY.ACTION_NOTIFY_APP_CRASH);
        intentFilter.addAction(TERMUX_ACTIVITY.ACTION_RELOAD_STYLE);
        intentFilter.addAction(TERMUX_ACTIVITY.ACTION_REQUEST_PERMISSIONS);

        registerReceiver(mTermuxActivityBroadcastReceiver, intentFilter);
    }

    private void unregisterTermuxActivityBroadcastReceiver() {
        unregisterReceiver(mTermuxActivityBroadcastReceiver);
    }

    private void fixTermuxActivityBroadcastReceiverIntent(Intent intent) {
        if (intent == null) return;

        String extraReloadStyle = intent.getStringExtra(TERMUX_ACTIVITY.EXTRA_RELOAD_STYLE);
        if ("storage".equals(extraReloadStyle)) {
            intent.removeExtra(TERMUX_ACTIVITY.EXTRA_RELOAD_STYLE);
            intent.setAction(TERMUX_ACTIVITY.ACTION_REQUEST_PERMISSIONS);
        }
    }

    class TermuxActivityBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;

            if (mIsVisible) {
                fixTermuxActivityBroadcastReceiverIntent(intent);

                switch (intent.getAction()) {
                    case TERMUX_ACTIVITY.ACTION_NOTIFY_APP_CRASH:
                        Logger.logDebug(LOG_TAG, "Received intent to notify app crash");
                        TermuxCrashUtils.notifyAppCrashFromCrashLogFile(context, LOG_TAG);
                        return;
                    case TERMUX_ACTIVITY.ACTION_RELOAD_STYLE:
                        Logger.logDebug(LOG_TAG, "Received intent to reload styling");
                        reloadActivityStyling(intent.getBooleanExtra(TERMUX_ACTIVITY.EXTRA_RECREATE_ACTIVITY, true));
                        return;
                    case TERMUX_ACTIVITY.ACTION_REQUEST_PERMISSIONS:
                        Logger.logDebug(LOG_TAG, "Received intent to request storage permissions");
                        requestStoragePermission(false);
                        return;
                    default:
                }
            }
        }
    }

    private void reloadActivityStyling(boolean recreateActivity) {
        if (mProperties != null) {
            reloadProperties();

            if (mExtraKeysView != null) {
                mExtraKeysView.setButtonTextAllCaps(mProperties.shouldExtraKeysTextBeAllCaps());
                mExtraKeysView.reload(mTermuxTerminalExtraKeys.getExtraKeysInfo(), mTerminalToolbarDefaultHeight);
            }

            // Update NightMode.APP_NIGHT_MODE
            TermuxThemeUtils.setAppNightMode(mProperties.getNightMode());
        }

        setMargins();
        setTerminalToolbarHeight();

        FileReceiverActivity.updateFileReceiverActivityComponentsState(this);

        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.onReloadActivityStyling();

        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onReloadActivityStyling();

        // To change the activity and drawer theme, activity needs to be recreated.
        // It will destroy the activity, including all stored variables and views, and onCreate()
        // will be called again. Extra keys input text, terminal sessions and transcripts will be preserved.
        if (recreateActivity) {
            Logger.logDebug(LOG_TAG, "Recreating activity");
            TermuxActivity.this.recreate();
        }
    }



    public static void startTermuxActivity(@NonNull final Context context) {
        ActivityUtils.startActivity(context, newInstance(context));
    }

    public static Intent newInstance(@NonNull final Context context) {
        Intent intent = new Intent(context, TermuxActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

}
