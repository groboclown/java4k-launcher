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
package net.javagaming.java4k.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * @author Groboclown
 */
public class LauncherBundle {
    private static Reference<ResourceBundle> bundle;
    private static final String BUNDLE = LauncherBundle.class.getName();

    private static Reference<Properties> spam;
    private static final String SPAM = "spamfilter.properties";

    private LauncherBundle() {
        // Intentionally empty
    }


    public static String message(String key, Object... params) {
        String val = getString(key);
        return String.format(val, params);
    }

    public static String getString(String key) {
        return getBundle().getString(key);
    }

    public static Map<String, String> getStringMapping() {
        ResourceBundle bundle = getBundle();
        Map<String, String> ret = new HashMap<String, String>();
        for (String key: bundle.keySet()) {
            try {
                String value = bundle.getString(key);
                if (value != null) {
                    ret.put(key, value);
                }
            } catch (Exception e) {
                // all kinds of things can go wrong here.  Ignore them.
            }
        }
        return ret;
    }


    public static boolean isFiltered(String category, int id) {
        return isFiltered(category, Integer.toString(id));
    }

    public static boolean isFiltered(String category, String id) {
        String property = category + '.' + id;
        String value = getSpamList().getProperty(property);
        return value != null;
    }



    private static ResourceBundle getBundle() {
        ResourceBundle ret = null;
        if (bundle != null) {
            ret = bundle.get();
        }
        if (ret == null) {
            ret = ResourceBundle.getBundle(BUNDLE);
            bundle = new SoftReference<ResourceBundle>(ret);
        }
        return ret;
    }

    private static Properties getSpamList() {
        Properties ret = null;
        if (spam != null) {
            ret = spam.get();
        }
        if (ret == null) {
            ret = new Properties();
            InputStream in = LauncherBundle.class.getResourceAsStream(SPAM);
            if (in != null) {
                try {
                    ret.load(in);
                } catch (IOException e) {
                    System.err.println("Could not load spam filter.");
                }
            }
            spam = new SoftReference<Properties>(ret);
        }
        return ret;
    }

}
