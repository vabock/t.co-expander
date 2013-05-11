
package jp.coffee_club.tco;

import android.os.AsyncTask;

import java.net.HttpURLConnection;
import java.net.URL;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

class ResultParam {
    /** redirect URL */
    public String url = null;
    /** error code */
    public int error = 0;
}

public class RedirectChecker extends AsyncTask<String, Void, ResultParam> {
    /** logcat tag */
    private static final String TAG = "RedirectChecker";

    /** result code: not redirect */
    public static final int RESULT_NOT_REDIRECT = R.string.msg_not_redirect;
    /** result code: error occur */
    public static final int RESULT_ERROR_OCCUR = R.string.msg_error_occur;

    /** result listener */
    private final OnResultListener mListener;

    private final String mUserAgent;
    
    /**
     * constructor
     *
     * @param listener
     */
    public RedirectChecker(String url, String userAgent, OnResultListener listener) {
        mListener = listener;
        mUserAgent = userAgent;
        execute(url);
    }

    /**
     * background procedure
     */
    @Override
    protected ResultParam doInBackground(String... params) {
        String url = params[0];
        ResultParam result = new ResultParam();

        try {
            // try connect
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            // no auto redirect
            con.setInstanceFollowRedirects(false);
            con.setRequestProperty("User-Agent", mUserAgent);

            // check response code
            int response = con.getResponseCode();
            debuglog("status: " + response);
            if (response == HttpURLConnection.HTTP_MOVED_PERM ||
                    response == HttpURLConnection.HTTP_MOVED_TEMP) {
                // redirect to Location: header URL
                result.url = con.getHeaderField("Location");
                debuglog("redirect: " + result.url);
            } else if (response == HttpURLConnection.HTTP_OK) {
                final String URL = "URL=";
                // not redirect
                Document doc = Jsoup.parse(con.getInputStream(), "UTF-8", url);
                Elements meta = doc.head().getElementsByTag("meta");
                
                for (Element elem : meta) {
                    if ("refresh".equalsIgnoreCase(elem.attr("http-equiv"))) {
                        String content = elem.attr("content");
                        int p = content.indexOf(URL);
                        
                        if (0 <= p) {
                            result.url = content.substring(p + URL.length());
                            debuglog("redirect(meta): " + result.url);
                            break;
                        }
                    }
                }
                
                if (result.url == null) {
                    result.error = RESULT_NOT_REDIRECT;
                }
            } else {
                result.error = RESULT_NOT_REDIRECT;
            }

        } catch (Exception e) {
            // error occur
            android.util.Log.e("debug", "error", e);
            result.error = RESULT_ERROR_OCCUR;
        }
        return result;
    }

    /**
     * post execute procedure
     */
    @Override
    protected void onPostExecute(ResultParam result) {
        // if cancelled, never come here.

        if (mListener != null) {
            if (result.error != 0) {
                // error occur
                mListener.onError(result.error);
            }

            if (result.url != null) {
                // redirect
                mListener.onRedirect(result.url);
            }
        }
    }

    /** result listener interface */
    public interface OnResultListener {
        /**
         * URL redirect event
         *
         * @param url
         */
        public void onRedirect(String url);

        /**
         * error occur event
         *
         * @param code
         */
        public void onError(int code);
    }

    /**
     * output debug log
     *
     * @param msg
     */
    private void debuglog(String msg) {
        if (BuildConfig.DEBUG) {
            android.util.Log.d(TAG, msg);
        }
    }
}
