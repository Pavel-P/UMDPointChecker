package nat.pettipaw.com.pointchecker;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.cengalabs.flatui.FlatUI;

import java.lang.annotation.Annotation;


public class LoginActivity extends Activity {

    TextView loginText;
    WebView view;
    Handler handler;
    LoginClient client;
    Button loginButton;

    @SuppressLint("JavascriptInterface")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        view = (WebView) findViewById(R.id.loginWebView);
        view.getSettings().setJavaScriptEnabled(true);
        view.clearCache(true);
        view.clearHistory();
        view.clearSslPreferences();
        view.clearFormData();
        view.addJavascriptInterface(new JavascriptLoginHandler(), "android");

        handler = new Handler();
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookies(new ValueCallback<Boolean>() {
            @Override
            public void onReceiveValue(Boolean value) {
            }
        });
        client = new LoginClient();
        view.setWebViewClient(client);

        View.OnTouchListener touchHideKeyboardListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                InputMethodManager inputMethodManager = (InputMethodManager) LoginActivity.this.getSystemService(Activity.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(LoginActivity.this.getCurrentFocus().getWindowToken(), 0);
                return false;
            }
        };

        findViewById(R.id.loginButton).setOnTouchListener(touchHideKeyboardListener);
        findViewById(R.id.baseLoginLayout).setOnTouchListener(touchHideKeyboardListener);

        loginText = (TextView) findViewById(R.id.loginText);
        loginButton = (Button) findViewById(R.id.loginButton);

    }

    public void onPressed(View v){

        client.loadTimes = 0;
        view.loadUrl("https://login.umd.edu/cas/login");
        loginButton.setEnabled(false);
        loginText.setText("Logging in...");

    }

    class LoginClient extends WebViewClient {
        int loadTimes = 0;

        @Override
        public void onPageFinished(WebView view, String url) {
            if (loadTimes == 0) {
                view.loadUrl("javascript:" +
                        "(function(){" +
                        "document.getElementById('username').value = '" + ((EditText) findViewById(R.id.username)).getText().toString() + "';" +
                        "document.getElementById('password').value = '" + ((EditText) findViewById(R.id.password)).getText().toString() + "';" +
                        "document.getElementsByName('submit')[0].click();" +
                        "})();");
            } else if (loadTimes == 1) {
                view.loadUrl("javascript:" +
                        "(function(){" +
                        "window.android.onFinishedLogin(document.getElementById('msg').className == 'success');" +
                        "})();");
            }
            loadTimes++;
        }
    }

    private class JavascriptLoginHandler {
        @android.webkit.JavascriptInterface
        public void onFinishedLogin(boolean success){

            if (success) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        loginText.setText("Logged in successfully.");
                    }
                }, 1);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(LoginActivity.this, PointViewerActivity.class);
                        startActivity(intent);
                    }
                }, 1500);
            } else {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        loginText.setText("Log in failed.");
                        loginButton.setEnabled(true);
                    }
                }, 1);
            }
        }
    }

}
