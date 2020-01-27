/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to &lt;http://unlicense.org/
 */
package net.javagaming.java4k.launcher.java4kcom;

import net.javagaming.java4k.launcher.AbstractCommentSubmission;
import net.javagaming.java4k.launcher.GameDetail;
import net.javagaming.java4k.launcher.LauncherBundle;
import net.javagaming.java4k.launcher.LauncherManager;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.StringTokenizer;

/**
 *
 *
 * @author Groboclown
 */
public class Java4kComCommentSubmission extends AbstractCommentSubmission {

    private static final String SET_COOKIE = "Set-Cookie";
    private static final String COOKIE_VALUE_DELIMITER = ";";
    private static final String HEADER_COOKIE = "Cookie";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String HEADER_CONTENT_LENGTH = "Content-Length";
    private static final char NAME_VALUE_SEPARATOR = '=';
    private static final String SESSION_COOKIE_ID = "PHPSESSID";
    private static final String HEADER_LOCATION = "Location";
    private static final String HEADER_REFERER = "Referer";
    private static final String HEADER_ACCEPT_CHARSET = "Accept-Charset";
    private static final String HEADER_CHARSET = "Charset";
    private static final String CHARSET = "UTF-8";


    private String cookieId;


    public Java4kComCommentSubmission(LauncherManager launcherManager) {
        super(launcherManager);
    }

    @Override
    protected String getSiteName() {
        return LauncherBundle.getString("java4k.site.dir");
    }

    @Override
    protected void sendAuthenticate(String user, String password) throws IOException {
        /*
         * First: request login page and capture the session cookie:
         * (always grab a new cookie)
         *
         * http://java4k.com/index.php?action=login&method=login
         *
         * GET /index.php?action=login&method=login HTTP/1.1
         * ...
         *
         * ----------------------
         *
         * HTTP/1.1 200 OK
         * Date: (date)
         * Set-Cookie: PHPSESSID=(cookie id); path=/
         * ...
         */
        {
            URL loginUrl = new URL(
                    LauncherBundle.getString("java4k.home.url"));
            //System.out.println("Connecting to " + loginUrl);
            HttpURLConnection urlConn = (HttpURLConnection) loginUrl.openConnection();
            try {
                urlConn.setUseCaches(false);
                InputStream response = urlConn.getInputStream();
                cookieId = loadSessionCookie(urlConn);
                drainResponse(response);
            } finally {
                urlConn.disconnect();
            }
        }



        /*
         * Then, send the actual login:
         * http://java4k.com/index.php?action=login&method=authenticate
         *
         * POST /index.php?action=login&method=authenticate HTTP/1.1
         * Referer: http://java4k.com/index.php?action=login&method=login
         * Cookie: PHPSESSID=(cookie id)
         * Content-Type: application/x-www-form-urlencoded
         * Content-Length: XXX
         * username=(userid)&password=(password)
         * ----
         * HTTP/1.1 302 Found
Date: Mon, 09 Dec 2013 21:50:17 GMT
Server: Apache
X-Powered-By: PHP/5.2.17
Expires: Thu, 19 Nov 1981 08:52:00 GMT
Cache-Control: no-store, no-cache, must-revalidate, post-check=0, pre-check=0
Pragma: no-cache
Location: index.php
Content-Length: 0
Connection: close
Content-Type: text/html
----------------------------------------------------------

         *
         * Login ok redirects the user to the front page, while login failure
         * sends the user back to the Referer page.
         *
         * However, this doesn't work right.
         */
        URL authenticateUrl = new URL(
                LauncherBundle.getString("java4k.authenticate.url"));
        // Note that the password is not encrypted. :(
        String data = "username=" + URLEncoder.encode(user, CHARSET) +
                "&password=" + URLEncoder.encode(password, CHARSET);
        byte[] dataBytes = data.getBytes(CHARSET);
        HttpURLConnection conn = (HttpURLConnection) authenticateUrl.openConnection();
        try {
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty(HEADER_ACCEPT_CHARSET, CHARSET);
            conn.setRequestProperty(HEADER_CHARSET, CHARSET);
            conn.setRequestProperty(HEADER_COOKIE,
                    SESSION_COOKIE_ID + '=' + cookieId);
            conn.setRequestProperty(HEADER_CONTENT_TYPE,
                    "application/x-www-form-urlencoded");
            conn.setRequestProperty(HEADER_REFERER,
                    LauncherBundle.getString("java4k.login.url"));
            conn.setRequestProperty(HEADER_CONTENT_LENGTH, Integer.toString(
                    dataBytes.length));
            conn.setUseCaches(false);

            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            try {
                wr.write(dataBytes);
                wr.flush();

                InputStream response = conn.getInputStream();

                // All logins, valid and unvaild, redirect you.
                if (conn.getResponseCode() != 302) {
                    throw new IOException("unexpected response code");
                }
                String location = loadLocationHeader(conn);
                //System.out.println("location:" + location);
                if (! location.equals(LauncherBundle.getString("java4k.authenticated.location"))) {
                    throw new IOException("login failed");
                }

                drainResponse(response);
            } finally {
                wr.close();
            }
        } finally {
            conn.disconnect();
        }
    }

    @Override
    protected void sendLogout() throws IOException {
        /*
         * http://java4k.com/index.php?action=login&method=logout
         *
         * GET /index.php?action=login&method=logout HTTP/1.1
         * Cookie: PHPSESSID=(cookie id))
         */
        URL authenticateUrl = new URL(
                LauncherBundle.getString("java4k.logout.url"));
        HttpURLConnection conn = (HttpURLConnection) authenticateUrl.openConnection();
        try {
            conn.setRequestProperty(HEADER_COOKIE,
                    SESSION_COOKIE_ID + '=' + cookieId);
            conn.setUseCaches(false);
            InputStream response = conn.getInputStream();
            drainResponse(response);

            cookieId = null;
        } finally {
            conn.disconnect();
        }
    }

    @Override
    protected void sendSubmit(GameDetail detail, String text) throws IOException {
        /*
         * <form name="comment" action="index.php?action=games&method=addcomment&gid=479" method="post">
         * <textarea name="comment" style="width:100%;height:10em;"></textarea>
         * <p><a href="#" onClick="this.disabled='disabled';document.forms['comment'].submit();" class="playnowbutton" style="width:120px;">Post Comment</a></p>
         * </form>
         */


        throw new IOException(
                "Comment submission is currently disabled until it is better tested.");
        /*
        URL submitURL = new URL(LauncherBundle.message("java4k.submit.url",
                detail.getSource().getId()));
        String data = "comment=" + URLEncoder.encode(text, CHARSET);
        byte[] dataBytes = data.getBytes(CHARSET);
        HttpURLConnection conn = (HttpURLConnection) submitURL.openConnection();
        try {
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty(HEADER_ACCEPT_CHARSET, CHARSET);
            conn.setRequestProperty(HEADER_CHARSET, CHARSET);
            conn.setRequestProperty(HEADER_COOKIE,
                    SESSION_COOKIE_ID + '=' + cookieId);
            conn.setRequestProperty(HEADER_CONTENT_TYPE,
                    "application/x-www-form-urlencoded");
            //conn.setRequestProperty(HEADER_REFERER,
            //        LauncherBundle.getString("java4k.login.url"));
            conn.setRequestProperty(HEADER_CONTENT_LENGTH, Integer.toString(
                    dataBytes.length));
            conn.setUseCaches(false);

            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            try {
                wr.write(dataBytes);
                wr.flush();

                InputStream response = conn.getInputStream();

                // All logins, valid and unvaild, redirect you.
                if (conn.getResponseCode() != 302) {
                    throw new IOException("unexpected response code");
                }
                // Don't know what to expect here to see if it was valid or not.
                String location = loadLocationHeader(conn);
System.out.println("location:" + location);
                //if (! location.equals(LauncherBundle.getString("java4k.authenticated.location"))) {
                //    throw new IOException("login failed");
                //}

                drainResponse(response);
            } finally {
                wr.close();
            }
        } finally {
            conn.disconnect();
        }
        */
    }

    /**
     *
     * @param conn the URL connection
     * @return non-null cookie value
     * @throws IOException thrown on a read error or if the cookie wasn't in the
     *      headers.
     */
    private String loadSessionCookie(URLConnection conn) throws IOException {
        String headerName;
        for (int i=1; (headerName = conn.getHeaderFieldKey(i)) != null; i++) {
            if (headerName.equalsIgnoreCase(SET_COOKIE)) {
                StringTokenizer st = new StringTokenizer(conn.getHeaderField(i), COOKIE_VALUE_DELIMITER);

                // the specification dictates that the first name/value pair
                // in the string is the cookie name and value, so let's handle
                // them as a special case:

                if (st.hasMoreTokens()) {
                    String token  = st.nextToken();
                    String name = token.substring(0, token.indexOf(NAME_VALUE_SEPARATOR));
                    String value = token.substring(token.indexOf(NAME_VALUE_SEPARATOR) + 1, token.length());
                    if (SESSION_COOKIE_ID.equals(name)) {
                        return value;
                    }
                }

                // Ignore the rest of the information (path, expires, etc)
            }
        }
        throw new IOException("No " + SESSION_COOKIE_ID +
                " cookie id found in response");
    }



    /**
     *
     * @param conn the URL connection
     * @return non-null cookie value
     * @throws IOException thrown on a read error or if the cookie wasn't in the
     *      headers.
     */
    private String loadLocationHeader(URLConnection conn)
            throws IOException {
        String headerName;
        // debug
        //for (int i=1; (headerName = conn.getHeaderFieldKey(i)) != null; i++) {
        //    System.out.println("header " + i + ": " + headerName + ": " + conn.getHeaderField(i));
        //}

        for (int i=1; (headerName = conn.getHeaderFieldKey(i)) != null; i++) {
            if (headerName.equalsIgnoreCase(HEADER_LOCATION)) {
                return conn.getHeaderField(i);
            }
        }
        throw new IOException("No location redirect header");
    }


    private void drainResponse(InputStream response) throws IOException {
        byte[] data = new byte[4096];
        int len;
        //System.out.print("Reponse: [");
        try {
            while ((len = response.read(data, 0, 4096)) > 0) {
                //System.out.print(new String(data, 0, len));
            }
        } finally {
            response.close();
        }
        //System.out.println("]");
    }
}
