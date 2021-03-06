/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/
package org.apache.cordova.inappbrowser;

import android.annotation.SuppressLint;
import org.apache.cordova.inappbrowser.InAppBrowserDialog;
import android.content.Context;
import android.content.Intent;
import android.provider.Browser;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.HttpAuthHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ProgressBar;
import android.graphics.drawable.GradientDrawable;
import android.view.MotionEvent;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.Config;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaHttpAuthHandler;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginManager;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Color;
import android.graphics.BitmapFactory;
import android.widget.ImageView;
import java.net.URL;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.StringTokenizer;

@SuppressLint("SetJavaScriptEnabled")

// public class MyWebViewClient extends WebViewClient {
//
//      @Override
//       public boolean shouldOverrideUrlLoading(WebView view, String url) {
//           view.loadUrl(url);
//           return false;
//       }
// }


public class InAppBrowser extends CordovaPlugin {

    private static final String NULL = "null";
    protected static final String LOG_TAG = "InAppBrowser";
    private static final String SELF = "_self";
    private static final String SYSTEM = "_system";
    private static final String EXIT_EVENT = "exit";
    private static final String LOCATION = "location";
    private static final String ZOOM = "zoom";
    private static final String HIDDEN = "hidden";
    private static final String LOAD_START_EVENT = "loadstart";
    private static final String LOAD_STOP_EVENT = "loadstop";
    private static final String LOAD_ERROR_EVENT = "loaderror";
    private static final String CLEAR_ALL_CACHE = "clearcache";
    private static final String CLEAR_SESSION_CACHE = "clearsessioncache";
    private static final String HARDWARE_BACK_BUTTON = "hardwareback";

    private InAppBrowserDialog dialog;
    private WebView inAppWebView;
    private EditText edittext;
    private CallbackContext callbackContext;
    private boolean showLocationBar = true;
    private boolean showZoomControls = true;
    private boolean openWindowHidden = false;
    private boolean clearAllCache = false;
    private boolean clearSessionCache = false;
    private boolean hadwareBackButton = true;

    public String optTitle = "";
    public String optColor = "";
    public String optBackground = "";
    public String optLoading = "";
    public String optBanner = "";
    public String optBannerUrl = "";




    public RelativeLayout progress;

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action the action to execute.
     * @param args JSONArry of arguments for the plugin.
     * @param callbackContext the callbackContext used when calling back into JavaScript.
     * @return A PluginResult object with a status and message.
     */
    public boolean execute(String action, CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
        if (action.equals("open")) {
            this.callbackContext = callbackContext;
            final String url = args.getString(0);

            optTitle = args.getString(3);
            optColor = args.getString(4);
            optBackground = args.getString(5);
            optLoading = args.getString(6);
            optBanner = args.getString(7);
            optBannerUrl = args.getString(8);

            String t = args.optString(1);
            if (t == null || t.equals("") || t.equals(NULL)) {
                t = SELF;
            }
            final String target = t;
            final HashMap<String, Boolean> features = parseFeature(args.optString(2));

            Log.d(LOG_TAG, "target = " + target);

            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String result = "";
                    // SELF
                    if (SELF.equals(target)) {
                        Log.d(LOG_TAG, "in self");
                        /* This code exists for compatibility between 3.x and 4.x versions of Cordova.
                         * Previously the Config class had a static method, isUrlWhitelisted(). That
                         * responsibility has been moved to the plugins, with an aggregating method in
                         * PluginManager.
                         */
                        Boolean shouldAllowNavigation = null;
                        if (url.startsWith("javascript:")) {
                            shouldAllowNavigation = true;
                        }
                        if (shouldAllowNavigation == null) {
                            try {
                                Method iuw = Config.class.getMethod("isUrlWhiteListed", String.class);
                                shouldAllowNavigation = (Boolean)iuw.invoke(null, url);
                            } catch (NoSuchMethodException e) {
                            } catch (IllegalAccessException e) {
                            } catch (InvocationTargetException e) {
                            }
                        }
                        if (shouldAllowNavigation == null) {
                            try {
                                Method gpm = webView.getClass().getMethod("getPluginManager");
                                PluginManager pm = (PluginManager)gpm.invoke(webView);
                                Method san = pm.getClass().getMethod("shouldAllowNavigation", String.class);
                                shouldAllowNavigation = (Boolean)san.invoke(pm, url);
                            } catch (NoSuchMethodException e) {
                            } catch (IllegalAccessException e) {
                            } catch (InvocationTargetException e) {
                            }
                        }
                        // load in webview
                        if (Boolean.TRUE.equals(shouldAllowNavigation)) {
                            Log.d(LOG_TAG, "loading in webview");
                            webView.loadUrl(url);
                        }
                        //Load the dialer
                        else if (url.startsWith(WebView.SCHEME_TEL))
                        {
                            try {
                                Log.d(LOG_TAG, "loading in dialer");
                                Intent intent = new Intent(Intent.ACTION_DIAL);
                                intent.setData(Uri.parse(url));
                                cordova.getActivity().startActivity(intent);
                            } catch (android.content.ActivityNotFoundException e) {
                                LOG.e(LOG_TAG, "Error dialing " + url + ": " + e.toString());
                            }
                        }
                        // load in InAppBrowser
                        else {
                            Log.d(LOG_TAG, "loading in InAppBrowser");
                            result = showWebPage(url, features);
                        }
                    }
                    // SYSTEM
                    else if (SYSTEM.equals(target)) {
                        Log.d(LOG_TAG, "in system");
                        result = openExternal(url);
                    }
                    // BLANK - or anything else
                    else {
                        Log.d(LOG_TAG, "in blank");
                        result = showWebPage(url, features);
                    }

                    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, result);
                    pluginResult.setKeepCallback(true);
                    callbackContext.sendPluginResult(pluginResult);
                }
            });
        }
        else if (action.equals("close")) {
            closeDialog();
        }
        else if (action.equals("injectScriptCode")) {
            String jsWrapper = null;
            if (args.getBoolean(1)) {
                jsWrapper = String.format("prompt(JSON.stringify([eval(%%s)]), 'gap-iab://%s')", callbackContext.getCallbackId());
            }
            injectDeferredObject(args.getString(0), jsWrapper);
        }
        else if (action.equals("injectScriptFile")) {
            String jsWrapper;
            if (args.getBoolean(1)) {
                jsWrapper = String.format("(function(d) { var c = d.createElement('script'); c.src = %%s; c.onload = function() { prompt('', 'gap-iab://%s'); }; d.body.appendChild(c); })(document)", callbackContext.getCallbackId());
            } else {
                jsWrapper = "(function(d) { var c = d.createElement('script'); c.src = %s; d.body.appendChild(c); })(document)";
            }
            injectDeferredObject(args.getString(0), jsWrapper);
        }
        else if (action.equals("injectStyleCode")) {
            String jsWrapper;
            if (args.getBoolean(1)) {
                jsWrapper = String.format("(function(d) { var c = d.createElement('style'); c.innerHTML = %%s; d.body.appendChild(c); prompt('', 'gap-iab://%s');})(document)", callbackContext.getCallbackId());
            } else {
                jsWrapper = "(function(d) { var c = d.createElement('style'); c.innerHTML = %s; d.body.appendChild(c); })(document)";
            }
            injectDeferredObject(args.getString(0), jsWrapper);
        }
        else if (action.equals("injectStyleFile")) {
            String jsWrapper;
            if (args.getBoolean(1)) {
                jsWrapper = String.format("(function(d) { var c = d.createElement('link'); c.rel='stylesheet'; c.type='text/css'; c.href = %%s; d.head.appendChild(c); prompt('', 'gap-iab://%s');})(document)", callbackContext.getCallbackId());
            } else {
                jsWrapper = "(function(d) { var c = d.createElement('link'); c.rel='stylesheet'; c.type='text/css'; c.href = %s; d.head.appendChild(c); })(document)";
            }
            injectDeferredObject(args.getString(0), jsWrapper);
        }
        else if (action.equals("show")) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dialog.show();
                }
            });
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
            pluginResult.setKeepCallback(true);
            this.callbackContext.sendPluginResult(pluginResult);
        }
        else {
            return false;
        }
        return true;
    }

    /**
     * Called when the view navigates.
     */
    @Override
    public void onReset() {
        closeDialog();
    }

    /**
     * Called by AccelBroker when listener is to be shut down.
     * Stop listener.
     */
    public void onDestroy() {
        closeDialog();
    }

    /**
     * Inject an object (script or style) into the InAppBrowser WebView.
     *
     * This is a helper method for the inject{Script|Style}{Code|File} API calls, which
     * provides a consistent method for injecting JavaScript code into the document.
     *
     * If a wrapper string is supplied, then the source string will be JSON-encoded (adding
     * quotes) and wrapped using string formatting. (The wrapper string should have a single
     * '%s' marker)
     *
     * @param source      The source object (filename or script/style text) to inject into
     *                    the document.
     * @param jsWrapper   A JavaScript string to wrap the source string in, so that the object
     *                    is properly injected, or null if the source string is JavaScript text
     *                    which should be executed directly.
     */
    private void injectDeferredObject(String source, String jsWrapper) {
        String scriptToInject;
        if (jsWrapper != null) {
            org.json.JSONArray jsonEsc = new org.json.JSONArray();
            jsonEsc.put(source);
            String jsonRepr = jsonEsc.toString();
            String jsonSourceString = jsonRepr.substring(1, jsonRepr.length()-1);
            scriptToInject = String.format(jsWrapper, jsonSourceString);
        } else {
            scriptToInject = source;
        }
        final String finalScriptToInject = scriptToInject;
        this.cordova.getActivity().runOnUiThread(new Runnable() {
            @SuppressLint("NewApi")
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                    // This action will have the side-effect of blurring the currently focused element
                    inAppWebView.loadUrl("javascript:" + finalScriptToInject);
                } else {
                    inAppWebView.evaluateJavascript(finalScriptToInject, null);
                }
            }
        });
    }

    /**
     * Put the list of features into a hash map
     *
     * @param optString
     * @return
     */
    private HashMap<String, Boolean> parseFeature(String optString) {
        if (optString.equals(NULL)) {
            return null;
        } else {
            HashMap<String, Boolean> map = new HashMap<String, Boolean>();
            StringTokenizer features = new StringTokenizer(optString, ",");
            StringTokenizer option;
            while(features.hasMoreElements()) {
                option = new StringTokenizer(features.nextToken(), "=");
                if (option.hasMoreElements()) {
                    String key = option.nextToken();
                    Boolean value = option.nextToken().equals("no") ? Boolean.FALSE : Boolean.TRUE;
                    map.put(key, value);
                }
            }
            return map;
        }
    }

    /**
     * Display a new browser with the specified URL.
     *
     * @param url the url to load.
     * @return "" if ok, or error message.
     */
    public String openExternal(String url) {
        try {
            Intent intent = null;
            intent = new Intent(Intent.ACTION_VIEW);
            // Omitting the MIME type for file: URLs causes "No Activity found to handle Intent".
            // Adding the MIME type to http: URLs causes them to not be handled by the downloader.
            Uri uri = Uri.parse(url);
            if ("file".equals(uri.getScheme())) {
                intent.setDataAndType(uri, webView.getResourceApi().getMimeType(uri));
            } else {
                intent.setData(uri);
            }
            intent.putExtra(Browser.EXTRA_APPLICATION_ID, cordova.getActivity().getPackageName());
            this.cordova.getActivity().startActivity(intent);
            return "";
        } catch (android.content.ActivityNotFoundException e) {
            Log.d(LOG_TAG, "InAppBrowser: Error loading url "+url+":"+ e.toString());
            return e.toString();
        }
    }

    /**
     * Closes the dialog
     */
    public void closeDialog() {
        final WebView childView = this.inAppWebView;
        // The JS protects against multiple calls, so this should happen only when
        // closeDialog() is called by other native code.
        if (childView == null) {
            return;
        }
        this.cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                childView.setWebViewClient(new WebViewClient() {
                    // NB: wait for about:blank before dismissing
                    public void onPageFinished(WebView view, String url) {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                    }
                });
                // NB: From SDK 19: "If you call methods on WebView from any thread
                // other than your app's UI thread, it can cause unexpected results."
                // http://developer.android.com/guide/webapps/migrating.html#Threads
                childView.loadUrl("about:blank");
            }
        });

        try {
            JSONObject obj = new JSONObject();
            obj.put("type", EXIT_EVENT);
            sendUpdate(obj, false);
        } catch (JSONException ex) {
            Log.d(LOG_TAG, "Should never happen");
        }
    }

    /**
     * Checks to see if it is possible to go back one page in history, then does so.
     */
    public void goBack() {
        if (this.inAppWebView.canGoBack()) {
            this.inAppWebView.goBack();
        }
    }

    /**
     * Can the web browser go back?
     * @return boolean
     */
    public boolean canGoBack() {
        return this.inAppWebView.canGoBack();
    }

    /**
     * Has the user set the hardware back button to go back
     * @return boolean
     */
    public boolean hardwareBack() {
        return hadwareBackButton;
    }

    /**
     * Checks to see if it is possible to go forward one page in history, then does so.
     */
    private void goForward() {
        if (this.inAppWebView.canGoForward()) {
            this.inAppWebView.goForward();
        }
    }

    /**
     * Navigate to the new page
     *
     * @param url to load
     */
    private void navigate(String url) {
        InputMethodManager imm = (InputMethodManager)this.cordova.getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(edittext.getWindowToken(), 0);

        if (!url.startsWith("http") && !url.startsWith("file:")) {
            this.inAppWebView.loadUrl("http://" + url);
        } else {
            this.inAppWebView.loadUrl(url);
        }
        this.inAppWebView.requestFocus();
    }


    /**
     * Should we show the location bar?
     *
     * @return boolean
     */
    private boolean getShowLocationBar() {
        return this.showLocationBar;
    }

    private InAppBrowser getInAppBrowser(){
        return this;
    }

    public static int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }
    /**
     * Display a new browser with the specified URL.
     *
     * @param url the url to load.
     * @param features jsonObject
     */
    public String showWebPage(final String url, HashMap<String, Boolean> features) {
        // Determine if we should hide the location bar.
        showLocationBar = true;
        showZoomControls = true;
        openWindowHidden = false;
        if (features != null) {
            Boolean show = features.get(LOCATION);
            if (show != null) {
                showLocationBar = show.booleanValue();
            }
            Boolean zoom = features.get(ZOOM);
            if (zoom != null) {
                showZoomControls = zoom.booleanValue();
            }
            Boolean hidden = features.get(HIDDEN);
            if (hidden != null) {
                openWindowHidden = hidden.booleanValue();
            }
            Boolean hardwareBack = features.get(HARDWARE_BACK_BUTTON);
            if (hardwareBack != null) {
                hadwareBackButton = hardwareBack.booleanValue();
            }
            Boolean cache = features.get(CLEAR_ALL_CACHE);
            if (cache != null) {
                clearAllCache = cache.booleanValue();
            } else {
                cache = features.get(CLEAR_SESSION_CACHE);
                if (cache != null) {
                    clearSessionCache = cache.booleanValue();
                }
            }
        }

        final CordovaWebView thatWebView = this.webView;

        // Create dialog in new thread
        Runnable runnable = new Runnable() {
            /**
             * Convert our DIP units to Pixels
             *
             * @return int
             */
            private int dpToPixels(int dipValue) {
                int value = (int) TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP,
                                                            (float) dipValue,
                                                            cordova.getActivity().getResources().getDisplayMetrics()
                );

                return value;
            }

            @SuppressLint("NewApi")
            public void run() {



                // Let's create the main dialog
                dialog = new InAppBrowserDialog(cordova.getActivity(), android.R.style.Theme_NoTitleBar);
                dialog.getWindow().getAttributes().windowAnimations = android.R.style.Animation_Dialog;
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setCancelable(true);
                dialog.setInAppBroswer(getInAppBrowser());


                WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
                lp.copyFrom(dialog.getWindow().getAttributes());
                lp.width = WindowManager.LayoutParams.MATCH_PARENT;
                lp.height = WindowManager.LayoutParams.MATCH_PARENT;

                // Main container layout
                LinearLayout main = new LinearLayout(cordova.getActivity());
                main.setOrientation(LinearLayout.VERTICAL);

                RelativeLayout toolbarContainer = new RelativeLayout(cordova.getActivity());
                toolbarContainer.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, this.dpToPixels(56)));
                toolbarContainer.setBackgroundColor(0xFF000000);

                // Toolbar layout

                RelativeLayout toolbar = new RelativeLayout(cordova.getActivity());
                //Please, no more black!
                toolbar.setBackgroundColor(Color.parseColor(optBackground));
                toolbar.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, this.dpToPixels(56)));
                toolbar.setPadding(this.dpToPixels(2), this.dpToPixels(2), this.dpToPixels(2), this.dpToPixels(2));
                toolbar.setHorizontalGravity(Gravity.LEFT);
                toolbar.setVerticalGravity(Gravity.TOP);

                //
                RelativeLayout bannerbar = new RelativeLayout(cordova.getActivity());
                //Please, no more black!
                bannerbar.setBackgroundColor(Color.parseColor(optBackground));


                // int bannerH = 56;
                int bannerH = (int)( getScreenWidth() / 6.4f );
                if (optBanner == null || optBanner.equals("") || optBanner.equals(NULL)){
                    bannerH = 0;
                }

                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, bannerH);
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                params.bottomMargin = 0;

                bannerbar.setLayoutParams(params);
                bannerbar.setPadding(this.dpToPixels(0), this.dpToPixels(0), this.dpToPixels(0), this.dpToPixels(0));
                bannerbar.setHorizontalGravity(Gravity.LEFT);
                bannerbar.setVerticalGravity(Gravity.TOP);
                bannerbar.setClickable(true);

                bannerbar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                         closeDialog();
                    };
                });




                GradientDrawable gd = new GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[] {0xFF000000,0x00000000});

                gd.setCornerRadius(0f);


                RelativeLayout toolbarShadow = new RelativeLayout(cordova.getActivity());
                toolbarShadow.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, this.dpToPixels(4)));
                toolbarShadow.setAlpha(0.5f);
                // toolbarContainer.setBackgroundDrawable(gd);
                // toolbar.setBackgroundDrawable(gd);
                toolbarShadow.setBackgroundDrawable(gd);
                // toolbar.setBackgroundDrawable(gd);

                ProgressBar progressBar = new ProgressBar(cordova.getActivity());

                progress = new RelativeLayout(cordova.getActivity());

                RelativeLayout.LayoutParams  preogresslayoutParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
                progress.setLayoutParams(preogresslayoutParams);
                progress.setHorizontalGravity(Gravity.CENTER);
                progress.setVerticalGravity(Gravity.CENTER);

                progress.addView(progressBar);

                // Action Button Container layout
                RelativeLayout actionButtonContainer = new RelativeLayout(cordova.getActivity());

                // int sizeInDPAction = 2;
                //
                // int marginInDpAction = (int) TypedValue.applyDimension(
                //             TypedValue.COMPLEX_UNIT_DIP, sizeInDPAction,cordova.getActivity().getResources()
                //                     .getDisplayMetrics());

                RelativeLayout.LayoutParams  actionslayoutParams = new RelativeLayout.LayoutParams(this.dpToPixels(41), this.dpToPixels(56));
                // actionslayoutParams.setMargins(marginInDpAction, 0, 0, 0);

                actionButtonContainer.setLayoutParams(actionslayoutParams);

                actionButtonContainer.setHorizontalGravity(Gravity.RIGHT);
                actionButtonContainer.setVerticalGravity(Gravity.CENTER_VERTICAL);
                actionButtonContainer.setId(Integer.valueOf(1));

                Resources activityRes = cordova.getActivity().getResources();


                edittext = new EditText(cordova.getActivity());


                TextView title = new TextView(cordova.getActivity());
                RelativeLayout.LayoutParams  titlelayoutParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT - this.dpToPixels(58), LayoutParams.MATCH_PARENT);


                int sizeInDP = 29;
                int marginInDp = (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, sizeInDP, cordova.getActivity().getResources()
                                    .getDisplayMetrics());

                titlelayoutParams.setMargins(marginInDp, 0, 0, 0);

                RelativeLayout.LayoutParams titleParams = titlelayoutParams;
                titleParams.addRule(RelativeLayout.CENTER_IN_PARENT);
                title.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
                titleParams.addRule(RelativeLayout.RIGHT_OF, 1);
                title.setId(Integer.valueOf(4));
                title.setLayoutParams(titleParams);
                title.setSingleLine(true);
                title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                title.setTextColor(Color.parseColor(optColor));
                title.setText(optTitle);



                // Close/Done button
                Button close = new Button(cordova.getActivity());
                RelativeLayout.LayoutParams closeLayoutParams = new RelativeLayout.LayoutParams( this.dpToPixels(28) , this.dpToPixels(28));
                closeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                close.setLayoutParams(closeLayoutParams);
                // forward.setContentDescription("Close Button");
                close.setId(Integer.valueOf(5));
                int closeResId;
                if(optColor.equals("#ffffff")){
                    closeResId = activityRes.getIdentifier("androidwhitearrowleft", "drawable", cordova.getActivity().getPackageName());
                } else {
                    closeResId = activityRes.getIdentifier("androidblackarrowleft", "drawable", cordova.getActivity().getPackageName());
                }

                Drawable closeIcon = activityRes.getDrawable(closeResId);

                if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN)
                {
                    close.setBackgroundDrawable(closeIcon);
                }
                else
                {
                    close.setBackground(closeIcon);
                }
                close.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        closeDialog();
                    }
                });


                RelativeLayout inappWithButtonContainer = new RelativeLayout(cordova.getActivity());
                inappWithButtonContainer.setBackgroundColor(Color.parseColor(optColor));

                inappWithButtonContainer.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
                inappWithButtonContainer.setHorizontalGravity(Gravity.LEFT);
                inappWithButtonContainer.setVerticalGravity(Gravity.TOP);




                RelativeLayout inappContainer = new RelativeLayout(cordova.getActivity());
                inappContainer.setBackgroundColor(Color.parseColor(optColor));

                inappContainer.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
                inappContainer.setHorizontalGravity(Gravity.LEFT);
                inappContainer.setVerticalGravity(Gravity.TOP);
                inappContainer.setPadding(this.dpToPixels(0), this.dpToPixels(0), this.dpToPixels(0), bannerH);


                // WebView
                inAppWebView = new WebView(cordova.getActivity());

                LinearLayout.LayoutParams paramsinAppWebView = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,  LayoutParams.MATCH_PARENT);
                paramsinAppWebView.bottomMargin = bannerH;

                inAppWebView.setLayoutParams(paramsinAppWebView);
                inAppWebView.setWebChromeClient(new InAppChromeClient(thatWebView));

                WebViewClient client = new InAppBrowserClient(thatWebView, edittext);
                inAppWebView.setWebViewClient(client);



                // inAppWebView.setMargins(this.dpToPixels(56), this.dpToPixels(56), this.dpToPixels(56), this.dpToPixels(56));
                WebSettings settings = inAppWebView.getSettings();
                settings.setJavaScriptEnabled(true);
                settings.setJavaScriptCanOpenWindowsAutomatically(true);
                settings.setBuiltInZoomControls(showZoomControls);
                settings.setPluginState(android.webkit.WebSettings.PluginState.ON);

                //Toggle whether this is enabled or not!
                Bundle appSettings = cordova.getActivity().getIntent().getExtras();
                boolean enableDatabase = appSettings == null ? true : appSettings.getBoolean("InAppBrowserStorageEnabled", true);
                if (enableDatabase) {
                    String databasePath = cordova.getActivity().getApplicationContext().getDir("inAppBrowserDB", Context.MODE_PRIVATE).getPath();
                    settings.setDatabasePath(databasePath);
                    settings.setDatabaseEnabled(true);
                }
                settings.setDomStorageEnabled(true);

                if (clearAllCache) {
                    CookieManager.getInstance().removeAllCookie();
                } else if (clearSessionCache) {
                    CookieManager.getInstance().removeSessionCookie();
                }

                inAppWebView.loadUrl(url);
                inAppWebView.setId(Integer.valueOf(6));
                inAppWebView.getSettings().setLoadWithOverviewMode(true);
                inAppWebView.getSettings().setUseWideViewPort(true);
                inAppWebView.requestFocus();
                inAppWebView.requestFocusFromTouch();


                String _htmlCon = "<html><head><title></title></head><body style=\"background-color:'#333'; margin:0px; padding:0px;\"><a href=\"" + optBannerUrl + "\"><img src=\"" + optBanner  + "\" style=\"width:100%;\" /></a></body></html>";

                WebView webviewBanner =  new WebView(cordova.getActivity());
                webviewBanner.getSettings().setJavaScriptEnabled(true);
                webviewBanner.loadDataWithBaseURL("", _htmlCon , "text/html", "UTF-8", "");

                webviewBanner.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                         closeDialog();
                    };
                });
                // Add the back and forward buttons to our action button container layout

                // actionButtonContainer.addView(forward);
                actionButtonContainer.addView(close);
                // Add the views to our toolbar
                toolbar.addView(actionButtonContainer);
                inAppWebView.addView(progress);
                inAppWebView.addView(toolbarShadow);

                toolbar.addView(title);
                // toolbar.addView(edittext);
                // toolbar.addView(close);

                // Don't add the toolbar if its been disabled
                if (getShowLocationBar()) {
                    // Add our toolbar to our main view/layout
                    // toolbarContainer.addView(toolbar);
                    // toolbarContainer.addView();
                    main.addView(toolbar);
                }

                // Add our webview to our main view/layout
                // main.addView(inAppWebView);


                inappContainer.addView(inAppWebView);
                bannerbar.addView(webviewBanner);


                inappWithButtonContainer.addView(inappContainer);
                inappWithButtonContainer.addView(bannerbar);

                main.addView(inappWithButtonContainer);


                dialog.setContentView(main);
                dialog.show();
                dialog.getWindow().setAttributes(lp);
                // the goal of openhidden is to load the url and not display it
                // Show() needs to be called to cause the URL to be loaded
                if(openWindowHidden) {
                    dialog.hide();
                }
            }
        };
        this.cordova.getActivity().runOnUiThread(runnable);
        return "";
    }

    /**
     * Create a new plugin success result and send it back to JavaScript
     *
     * @param obj a JSONObject contain event payload information
     */
    private void sendUpdate(JSONObject obj, boolean keepCallback) {
        sendUpdate(obj, keepCallback, PluginResult.Status.OK);
    }

    /**
     * Create a new plugin result and send it back to JavaScript
     *
     * @param obj a JSONObject contain event payload information
     * @param status the status code to return to the JavaScript environment
     */
    private void sendUpdate(JSONObject obj, boolean keepCallback, PluginResult.Status status) {
        if (callbackContext != null) {
            PluginResult result = new PluginResult(status, obj);
            result.setKeepCallback(keepCallback);
            callbackContext.sendPluginResult(result);
            if (!keepCallback) {
                callbackContext = null;
            }
        }
    }

    /**
     * The webview client receives notifications about appView
     */
    public class InAppBrowserClient extends WebViewClient {
        EditText edittext;
        CordovaWebView webView;



        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            //
            if (url.startsWith("newtab:")) {
                view.getContext().startActivity(
                new Intent(Intent.ACTION_VIEW, Uri.parse(url.replace("newtab:", ""))));
           }
           else {
               view.loadUrl(url); //load url in current WebView
           }
           return true;
            // if (url != null  && url.toLowerCase().contains(".pdf")) {
            //     view.getContext().startActivity(
            //         new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            //     return true;
            // } else {
            //     return false;
            // }
            // WebView.HitTestResult result = view.getHitTestResult();
            // String data = result.getExtra();
            // Context context = view.getContext();
            // Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(data));
            // context.startActivity(browserIntent);
            // return false;
        }
        /**
         * Constructor.
         *
         * @param webView
         * @param mEditText
         */
        public InAppBrowserClient(CordovaWebView webView, EditText mEditText) {
            this.webView = webView;
            this.edittext = mEditText;
        }

        /**
         * Notify the host application that a page has started loading.
         *
         * @param view          The webview initiating the callback.
         * @param url           The url of the page.
         */
        @Override
        public void onPageStarted(WebView view, String url,  Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            String newloc = "";
            if (url.startsWith("http:") || url.startsWith("https:") || url.startsWith("file:")) {
                newloc = url;
            }
            else if (url.startsWith("rtsp:") || url.startsWith("rtps:")) {
                try {


                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    cordova.getActivity().startActivity(i);
                    closeDialog();

                } catch (android.content.ActivityNotFoundException e) {
                    LOG.e(LOG_TAG, "Error with " + url + ": " + e.toString());
                }
            }
            // If dialing phone (tel:5551212)

            else if (url.startsWith(WebView.SCHEME_TEL)) {
                try {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse(url));
                    cordova.getActivity().startActivity(intent);
                } catch (android.content.ActivityNotFoundException e) {
                    LOG.e(LOG_TAG, "Error dialing " + url + ": " + e.toString());
                }
            }



            else if (url.startsWith("geo:") || url.startsWith(WebView.SCHEME_MAILTO) || url.startsWith("market:")) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    cordova.getActivity().startActivity(intent);
                } catch (android.content.ActivityNotFoundException e) {
                    LOG.e(LOG_TAG, "Error with " + url + ": " + e.toString());
                }
            }
            // If sms:5551212?body=This is the message
            else if (url.startsWith("sms:")) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);

                    // Get address
                    String address = null;
                    int parmIndex = url.indexOf('?');
                    if (parmIndex == -1) {
                        address = url.substring(4);
                    }
                    else {
                        address = url.substring(4, parmIndex);

                        // If body, then set sms body
                        Uri uri = Uri.parse(url);
                        String query = uri.getQuery();
                        if (query != null) {
                            if (query.startsWith("body=")) {
                                intent.putExtra("sms_body", query.substring(5));
                            }
                        }
                    }
                    intent.setData(Uri.parse("sms:" + address));
                    intent.putExtra("address", address);
                    intent.setType("vnd.android-dir/mms-sms");
                    cordova.getActivity().startActivity(intent);
                } catch (android.content.ActivityNotFoundException e) {
                    LOG.e(LOG_TAG, "Error sending sms " + url + ":" + e.toString());
                }
            }
            else {
                newloc = "http://" + url;
            }

            if (!newloc.equals(edittext.getText().toString())) {
                edittext.setText(newloc);
            }

            try {
                JSONObject obj = new JSONObject();
                obj.put("type", LOAD_START_EVENT);
                obj.put("url", newloc);

                sendUpdate(obj, true);
            } catch (JSONException ex) {
                Log.d(LOG_TAG, "Should never happen");
            }
        }

        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            view.loadUrl("javascript: var allLinks = document.getElementsByTagName('a'); function findlinks(){if (allLinks) {for (var i=0; i<allLinks.length; i++) {var link = allLinks[i]; var target = link.getAttribute('target'); if (target && target == '_blank') { link.href = 'newtab:'+link.href; } } } }findlinks();");
            progress.setVisibility(LinearLayout.GONE);
            try {
                JSONObject obj = new JSONObject();
                obj.put("type", LOAD_STOP_EVENT);
                obj.put("url", url);

                sendUpdate(obj, true);
            } catch (JSONException ex) {
                Log.d(LOG_TAG, "Should never happen");
            }
        }

        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);

            try {
                JSONObject obj = new JSONObject();
                obj.put("type", LOAD_ERROR_EVENT);
                obj.put("url", failingUrl);
                obj.put("code", errorCode);
                obj.put("message", description);

                sendUpdate(obj, true, PluginResult.Status.ERROR);
            } catch (JSONException ex) {
                Log.d(LOG_TAG, "Should never happen");
            }
        }

        /**
         * On received http auth request.
         */
        @Override
        public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {

            // Check if there is some plugin which can resolve this auth challenge
            PluginManager pluginManager = null;
            try {
                Method gpm = webView.getClass().getMethod("getPluginManager");
                pluginManager = (PluginManager)gpm.invoke(webView);
            } catch (NoSuchMethodException e) {
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e) {
            }

            if (pluginManager == null) {
                try {
                    Field pmf = webView.getClass().getField("pluginManager");
                    pluginManager = (PluginManager)pmf.get(webView);
                } catch (NoSuchFieldException e) {
                } catch (IllegalAccessException e) {
                }
            }

            if (pluginManager != null && pluginManager.onReceivedHttpAuthRequest(webView, new CordovaHttpAuthHandler(handler), host, realm)) {
                return;
            }

            // By default handle 401 like we'd normally do!
            super.onReceivedHttpAuthRequest(view, handler, host, realm);
        }



    }


}
