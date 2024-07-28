package com.termux.app;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.autofill.AutofillManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.webkit.WebView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.termux.LoginActivity;
import com.termux.R;
import com.termux.ServerStatusChecker;
import com.termux.SetupActivity;
import com.termux.SkyActionActivity;
import com.termux.SkySharedPref;
import com.termux.WebPlayerActivity;
import com.termux.app.api.file.FileReceiverActivity;
import com.termux.app.terminal.TermuxActivityRootView;
import com.termux.app.terminal.TermuxTerminalSessionActivityClient;
import com.termux.app.terminal.io.TermuxTerminalExtraKeys;
import com.termux.shared.activities.ReportActivity;
import com.termux.shared.activity.ActivityUtils;
import com.termux.shared.activity.media.AppCompatActivityUtils;
import com.termux.shared.data.IntentUtils;
import com.termux.shared.android.PermissionUtils;
import com.termux.shared.data.DataUtils;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_ACTIVITY;
import com.termux.app.activities.HelpActivity;
import com.termux.app.activities.SettingsActivity;
import com.termux.shared.termux.crash.TermuxCrashUtils;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.app.terminal.TermuxSessionsListViewController;
import com.termux.app.terminal.io.TerminalToolbarViewPager;
import com.termux.app.terminal.TermuxTerminalViewClient;
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
import com.termux.view.TerminalView;
import com.termux.view.TerminalViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager.widget.ViewPager;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
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

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String DOWNLOAD_URL = "http://localhost:5001/playlist.m3u";

    private TextView ipAddressTextView;
    private TextView serverStatusTextView;
    private ServerStatusChecker serverStatusChecker;


    private boolean isCanceled = false;


    private Runnable autoDismissRunnable;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Logger.logDebug(LOG_TAG, "onCreate");
        mIsOnResumeAfterOnCreate = true;

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
        handler = new Handler();


        Button button1 = findViewById(R.id.button1);
        Button button1_5 = findViewById(R.id.button1_5);
        Button button2 = findViewById(R.id.button2);
//        Button button3 = findViewById(R.id.button3);
//        Button button4 = findViewById(R.id.button4);
//        Button button5 = findViewById(R.id.button5);
        Button button6 = findViewById(R.id.button6);
        Button button7 = findViewById(R.id.button7);
        Button button8 = findViewById(R.id.button8);

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
        button2.setOnFocusChangeListener(tooltipFocusListener);
//        button3.setOnFocusChangeListener(tooltipFocusListener);
//        button4.setOnFocusChangeListener(tooltipFocusListener);
//        button5.setOnFocusChangeListener(tooltipFocusListener);
        button6.setOnFocusChangeListener(tooltipFocusListener);
        button7.setOnFocusChangeListener(tooltipFocusListener);
        button8.setOnFocusChangeListener(tooltipFocusListener);


        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle button1 click
                sky_rerun();
            }
        });

        button1_5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //sky_tv();
//                Intent intent = new Intent(TermuxActivity.this, com.termux.VideoPlayerActivity.class);
//                startActivity(intent);

                Intent intent = new Intent(TermuxActivity.this, WebPlayerActivity.class);
                startActivity(intent);
            }
        });

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(TermuxActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });


//        button2.setOnClickListener(new View.OnClickListener() {
//
//
//            Intent intent = new Intent(TermuxActivity.this, LoginActivity.class);
//            startActivity(intent);
//
////            @SuppressLint("SetJavaScriptEnabled")
////            @Override
////            public void onClick(View v) {
////                if (isWebViewVisible) {
////                    webView.setVisibility(View.GONE);
////                } else {
////                    webView.setVisibility(View.VISIBLE);
////                    webView.setWebChromeClient(new WebChromeClient());
////
////                    WebSettings webSettings = webView.getSettings();
////                    webSettings.setJavaScriptEnabled(true);
////
////                    webView.loadUrl("http://localhost:5001");
////
////                    // Add a WebViewClient to execute JavaScript once the page is loaded
////                    webView.setWebViewClient(new WebViewClient() {
////                        @Override
////                        @Override
//public void onPageFinished(WebView view, String url) {
//    // Inject JavaScript to modify innerHTML and open/center the modal
//    view.evaluateJavascript(
//        "document.body.innerHTML = '<div id=\"login_modal\">' + document.getElementById('login_modal').outerHTML + '</div>';" +
//        "document.body.style.overflow = 'hidden'; " + // Prevent scrolling
//        "var modal = document.getElementById('login_modal'); " +
//        "if (modal) { " +
//        "  modal.style.display = 'block'; " + // Ensure the modal is displayed
//        "  modal.style.position = 'fixed'; " + // Fix position relative to the viewport
//        "  modal.style.top = '50%'; " + // Center vertically
//        "  modal.style.left = '50%'; " + // Center horizontally
//        "  modal.style.transform = 'translate(-50%, -50%)'; " + // Offset for exact centering
//        "  modal.style.zIndex = '10000'; " + // Ensure it’s on top of other content
//        "}",
//        null
//    );
//}
////                }
////                isWebViewVisible = !isWebViewVisible;
////        }
//        });



//
//        button3.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Handle button3 click
//                sky_iptv();
//            }
//        });

//        button4.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Handle button4 click
//                sky_update();
//            }
//        });

//        button5.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Handle button5 click
//                sky_reinstall();
//            }
//        });

//        button6.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                 Handle button5 click
//                sky_net();
//                sky_runcode();
//            }
//        });


        button6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an AlertDialog Builder with the custom style
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext(), R.style.CustomAlertDialogTheme);

                // Set the dialog title
                builder.setTitle("Choose an option");

                // Add a radio button list
                String[] options = {"Update JioTV Go", "Reinstall","Run Code","Switch to Terminal"};
                final int[] selectedOption = {-1}; // Store the selected option

                builder.setSingleChoiceItems(options, selectedOption[0], new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Store the selected option
                        selectedOption[0] = which;
                    }
                });

                // Add OK and Cancel buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // User clicked OK button
                        // Handle the selected option
                        switch (selectedOption[0]) {
                            case 0:
                                // Option 1 selected
                                sky_update();
                                break;
                            case 1:
                                // Option 2 selected
                                sky_reinstall();
                                break;
                            case 2:
                                // Option 3 selected
                                sky_runcode();
                                break;
                            case 3:
                                // Option 4 selected
                                lake_alert_confirmation(v.getContext());
                                break;
                            default:
                                // No option selected or invalid
                                break;
                        }
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // User cancelled the dialog
                        dialog.dismiss();
                    }
                });

                // Create and show the AlertDialog
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });


        button7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle button7 click -
                sky_exit();

//                Intent intent = new Intent(TermuxActivity.this, AppSelectorActivity.class);
//                startActivity(intent);


//                Intent intent = new Intent(TermuxActivity.this, SetupActivity.class);
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
        webView = findViewById(R.id.webview_tv);

        webView.setVisibility(View.GONE); // Initially hide the WebView
        imageView.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetJavaScriptEnabled")
            @Override
            public void onClick(View v) {
                if (isWebViewVisible) {
                    webView.setVisibility(View.GONE);
                } else {
                    webView.setVisibility(View.VISIBLE);
                    webView.setWebChromeClient(new WebChromeClient());

                    WebSettings webSettings = webView.getSettings();
                    webSettings.setJavaScriptEnabled(true);
                    webView.loadUrl("http://localhost:5001");
                }
                isWebViewVisible = !isWebViewVisible;

//                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://localhost:5001/"));
//                startActivity(browserIntent);
            }
        });

        // Check and request MANAGE_OVERLAY_PERMISSION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                overapp_confirmation(this);

            } else {
                // Permission already granted, proceed with the app logic
                proceedWithAppLogic();
            }
        }


        ipAddressTextView = findViewById(R.id.ip_address);
        serverStatusTextView = findViewById(R.id.server_status);

        SkySharedPref preferenceManager = new SkySharedPref(TermuxActivity.this);
        String isLOCAL = preferenceManager.getKey("server_setup_isLocal");

        if (Objects.equals(isLOCAL, "Yes")){
            Log.d("d","Server is Local!");
        } else {
            // Get and display Wi-Fi IP address
            String wifiIpAddress = getWifiIpAddress(this);
            ipAddressTextView.setText(wifiIpAddress);
        }




        // Start checking server status
        serverStatusChecker = new ServerStatusChecker(serverStatusTextView);
        serverStatusChecker.startChecking();


        ipAddressTextView = findViewById(R.id.ip_address);
        downloadIcon = findViewById(R.id.ic_download);

        downloadIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPermission()) {
                    downloadFile(DOWNLOAD_URL);
                } else {
                    requestPermission();
                }
            }
        });










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
            Logger.logStackTraceWithMessage(LOG_TAG,"TermuxActivity failed to start TermuxService", e);
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

    private void sky_config() {
        Intent intent = new Intent(TermuxActivity.this, SetupActivity.class);
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
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                //EMPTY
            }
        };
        handler.postDelayed(runnable, 100);
    }

    public void wait_special() {
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                //EMPTY
            }
        };
        handler.postDelayed(runnable, 1000);
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
        wait_();
    }

    private void XStartTermux() {
        // Start the Termux service
        Intent termuxServiceStartIntent = new Intent();
        termuxServiceStartIntent.setClassName("com.termux", "com.termux.app.TermuxService");
        termuxServiceStartIntent.setAction("com.termux.service_execute");
        startService(termuxServiceStartIntent);
        wait_();
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


    public void wait_X() {
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                //EMPTY
            }
        };
        handler.postDelayed(runnable, 2000);
    }
    private void sky_rerun() {
        Toast.makeText(this, "Re-Running CustTermux", Toast.LENGTH_SHORT).show();
        XpkillIntent();
        XpkillIntentbg();
        //XStopTermux();

        //XStartTermuxAct();

        wait_X();

        //XStartEMPTY();
        //XStartIPTV();
        XStartTermux();
        //launchTermux();

    }
//    private void sky_login() {
//        Intent intentC = new Intent();
//        intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
//        intentC.setAction("com.termux.RUN_COMMAND");
//        intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
//        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"login"});
//        intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
//        intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
//        intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
//        startService(intentC);
//    }


    private void sky_login() {
        // Create an AlertDialog Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Phone Number To Login");

        // Set up the input field
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Retrieve the phone number from the input field
                phoneNumber = input.getText().toString();

                // Log the phone number
                //Log.d("SkyLogin", "Phone Number: " + phoneNumber);

                // Call the nested function and pass the phone number
                handleSendOTP(phoneNumber);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        // Create and show the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void sky_otp() {
        // Create an AlertDialog Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter OTP");

        // Set up the input field
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)}); // OTP is 6 digits
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Retrieve the OTP from the input field
                otp = input.getText().toString();

                // Log the OTP
                //Log.d("SkyOTP", "OTP: " + otp);

                // Call the nested function and pass the OTP
                handleVerifyOTP(otp);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        // Create and show the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Nested function to handle the phone number
    private void handleSendOTP(String phoneNumber) {
        // Your code to handle the phone number
        //Log.d("HandlePhoneNumber", "Phone Number in handleSendOTP: " + phoneNumber);
        // Start the Start IPTV service
        Intent intentC = new Intent();
        intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
        intentC.setAction("com.termux.RUN_COMMAND");
        intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"sendotp",phoneNumber});
        intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
        intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        startService(intentC);

        wait_special();

        sky_otp();
    }

    private void handleVerifyOTP(String otp) {
        // Your code to handle the OTP
        //Log.d("HandleOTP", "OTP in handleVerifyOTP: " + otp);
        Intent intentO = new Intent();
        intentO.setClassName("com.termux", "com.termux.app.RunCommandService");
        intentO.setAction("com.termux.RUN_COMMAND");
        intentO.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
        intentO.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"verifyotp",phoneNumber,otp});
        intentO.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentO.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
        intentO.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        startService(intentO);
    }



    private void sky_iptv() {
        Intent intentC = new Intent();
        intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
        intentC.setAction("com.termux.RUN_COMMAND");
        intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"iptv"});
        intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
        intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        startService(intentC);
    }
    private void sky_update() {
        Intent intentC = new Intent();
        intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
        intentC.setAction("com.termux.RUN_COMMAND");
        intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"update"});
        intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
        intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        startService(intentC);
    }

    private void sky_reinstall() {
        Intent intentC = new Intent();
        intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
        intentC.setAction("com.termux.RUN_COMMAND");
        intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"reinstall"});
        intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
        intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        startService(intentC);
    }

    private void sky_runcode() {
        Intent intentC = new Intent();
        intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
        intentC.setAction("com.termux.RUN_COMMAND");
        intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"runcode"});
        intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
        intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        startService(intentC);
    }

    private void sky_exit() {
        //loginchecker();
        XStopTermux();



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


    private void sky_terminal() {
        TerminalView terminalView = findViewById(R.id.terminal_view);

        // Change focusable properties
        terminalView.setFocusableInTouchMode(true);
        terminalView.setFocusable(true);
    }

    public void loginchecker() {
        // URL to check
        String url = "http://localhost:5001/live/144.m3u8";

        // Execute AsyncTask to check status code
        new CheckStatusTask().execute(url);
    }

    private class CheckStatusTask extends AsyncTask<String, Void, Integer> {

        @Override
        protected Integer doInBackground(String... urls) {
            String urlString = urls[0];
            HttpURLConnection connection = null;
            try {
                URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                return connection.getResponseCode();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }

        @Override
        protected void onPostExecute(Integer responseCode) {
            if (responseCode != null) {
                // Handle the response code
                switch (responseCode) {
                    case HttpURLConnection.HTTP_OK:
                        System.out.println("The webpage is accessible.");
                        break;
                    case HttpURLConnection.HTTP_NOT_FOUND:
                        System.out.println("The webpage was not found.");
                        break;
                    default:
                        System.out.println("Response code: " + responseCode);
                        break;
                }
            } else {
                System.out.println("Error occurred while checking status code.");
            }
        }
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


    private void downloadFile(String fileUrl) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(fileUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.connect();

                    InputStream inputStream = new BufferedInputStream(connection.getInputStream());
                    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "playlist.m3u");
                    FileOutputStream outputStream = new FileOutputStream(file);

                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }

                    outputStream.close();
                    inputStream.close();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(TermuxActivity.this, "Playlist downloaded to Downloads folder", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
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

    //////////////////////////////////////////////////////////////////////////////////////





    @Override
    public void onStart() {
        super.onStart();

        SkySharedPref preferenceManager = new SkySharedPref(this);
        String serverSetupDone = preferenceManager.getKey("isServerSetupDone");

        if (serverSetupDone != null && serverSetupDone.equals("Done")) {
            //sky_exit();
        } else {
            Toast.makeText(TermuxActivity.this, "setup starts here", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(TermuxActivity.this, SetupActivity.class);
            startActivity(intent);

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





    @SuppressLint("StaticFieldLeak")
    private class CheckUrlTask extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... urls) {
            String urlString = urls[0];
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(urlString);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                int responseCode = urlConnection.getResponseCode();
                return (responseCode == HttpURLConnection.HTTP_OK);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        }



        @Override
        protected void onPostExecute(Boolean isUrlAvailable) {
            if (isUrlAvailable) {
                SkySharedPref preferenceManager = new SkySharedPref(TermuxActivity.this);
                String iptv_checker = preferenceManager.getKey("app_name");

                if (iptv_checker != null && !iptv_checker.isEmpty()) {
                    if (iptv_checker.equals("null")) {
                        System.out.println("IPTV, null!");
                    } else if (iptv_checker.equals("sky_web_tv")) {
                        System.out.println("IPTV, webTV!");
                        Intent intent = new Intent(TermuxActivity.this, WebPlayerActivity.class);
                        startActivity(intent);
                        openIptvCount++;
                    } else {
                        System.out.println("IPTV, found!");
                        showAlert();
                        openIptvCount++;
                    }
                } else {
                    System.out.println("IPTV, null!");
                }

            } else {
                // Handle the case when the URL is not available
                Toast.makeText(TermuxActivity.this, "Server not available.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private int openIptvCount = 0;
    private final int maxOpenIptvCalls = 10;

    @Override
    public void onResume() {
        super.onResume();

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



        
        
        if (openIptvCount < maxOpenIptvCalls) {
            new CheckUrlTask().execute("http://localhost:5001");

        }


    }

    private void showAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.iptv_alert, null);

        // Retrieve the icon from SharedPreferences
        SkySharedPref preferenceManager = new SkySharedPref(this);
        String iconBase64 = preferenceManager.getKey("app_icon");

        ImageView iconImageView = dialogView.findViewById(R.id.iptv_icon);
        if (iconBase64 != null) {
            // Convert base64 string to Bitmap
            Bitmap iconBitmap = base64ToBitmap(iconBase64);
            iconImageView.setImageBitmap(iconBitmap);
        } else {
            // Set default icon
            iconImageView.setImageResource(R.mipmap.ic_launcher2);
        }

        // Retrieve the package name from SharedPreferences
        String packageName = preferenceManager.getKey("app_name");

        // Use PackageManager to get the application name
        String appName = getAppNameFromPackageName(packageName);

        // Set the application name in the TextView
        TextView iptvNameTextView = dialogView.findViewById(R.id.iptv_name);
        iptvNameTextView.setText("Opening " + appName);

        // Set the countdown timer in the TextView
        TextView countdownTextView = dialogView.findViewById(R.id.countdown_timer);
        final int countdownDuration = 4000; // 4 seconds
        countdownTextView.setText((countdownDuration / 1000) + "s");

        AlertDialog alertDialog = builder.create();
        alertDialog.setView(dialogView); // Set the inflated layout as the dialog content

        Button dismissButton = dialogView.findViewById(R.id.dismiss_button);
        dismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
                // Handle button click logic here
            }
        });

        alertDialog.show();

        dismissButton.post(new Runnable() {
            @Override
            public void run() {
                dismissButton.requestFocus();
            }
        });

        // Countdown timer logic
        new CountDownTimer(countdownDuration, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                countdownTextView.setText((millisUntilFinished / 1000) + "s");
            }

            @Override
            public void onFinish() {
                if (alertDialog.isShowing()) {
                    alertDialog.dismiss();
                    onDialogTimeout();
                }
            }
        }.start();
    }

    // Convert Base64 to Bitmap
    public static Bitmap base64ToBitmap(String base64String) {
        byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

    // Get application name from package name
    private String getAppNameFromPackageName(String packageName) {
        PackageManager packageManager = getPackageManager();
        try {
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
            return packageManager.getApplicationLabel(appInfo).toString();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return packageName; // If not found, return the package name itself
        }
    }

    private void onDialogTimeout() {
        // Logic to execute if the dialog is not interacted with within the timeout duration
        Toast.makeText(this, "Dialog dismissed due to timeout", Toast.LENGTH_SHORT).show();
    }







    private void showAlert1() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Alert")
            .setMessage("Do you want to proceed?")
            .setCancelable(false)
            .setPositiveButton("Proceed", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Proceed with starting IPTV
                    //iptv_check();
                    XStartIPTV();
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    isCanceled = true;
                    //stopStartingOPTV(); // Called when user presses Cancel
                }
            });

        final AlertDialog dialog = builder.create();
        dialog.show();

        // Ensure that the Handler and Runnable are not null before scheduling
        if (handler != null) {
            autoDismissRunnable = new Runnable() {
                @Override
                public void run() {
                    if (!isCanceled) {
                        dialog.dismiss();
                        //iptv_check(); // Proceed if not canceled
                        XStartIPTV();
                    }
                }
            };
            handler.postDelayed(autoDismissRunnable, 4000); // 4000 milliseconds = 4 seconds
        }
    }

    private void iptv_check() {
        if (openIptvCount < maxOpenIptvCalls) {
            new CheckUrlTask().execute("http://localhost:5001");
        }
    }


    public String getWifiIpAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        return Formatter.formatIpAddress(ipAddress);
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

        serverStatusChecker.stopChecking();

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
                    TermuxActivity.this, requestCode, !isPermissionCallback)) {
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
                downloadFile(DOWNLOAD_URL);
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
