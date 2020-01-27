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
package net.javagaming.java4k.launcher.cache;

import net.javagaming.java4k.launcher.DefaultGameDetailListResourceReader;
import net.javagaming.java4k.launcher.GameConfiguration;
import net.javagaming.java4k.launcher.GameDescription;
import net.javagaming.java4k.launcher.GameDescriptionListResourceReader;
import net.javagaming.java4k.launcher.GameDetail;
import net.javagaming.java4k.launcher.LauncherManager;
import net.javagaming.java4k.launcher.java4kcom.Java4kComYearCategoryListReader;
import net.javagaming.java4k.launcher.progress.SimpleResourceProducer;

import java.io.IOException;
import java.util.List;

/**
 * A main program for populating the cache.
 *
 *
 * @author Groboclown
 */
public class CachePopulator {
    private static final long DELAY = 100L;


    public static void main(String args[]) throws InterruptedException {
        // First, download all the known troublemakers.
        // This should be an option - download just the troublemakers, or
        // download everything.

        for (String uri: Cache.getInstance().getUrlMap().getRedirectedUris()) {
            Resource r = null;
            try {
                r = Cache.getInstance().getResource(null,
                        Cache.uri(uri), true);
            } catch (IOException e) {
                System.err.println("[ERROR] *** Bad url: " + uri);
            }
            if (r == null) {
                continue;
            }
            try {
                r.read().close();
            } catch (IOException e) {
                System.err.println("[ERROR] *** Could not load " + uri);
            }
        }

        ThreadGroup tg = new ThreadGroup("");
        Java4kComYearCategoryListReader categoryFactory;
        try {
            categoryFactory = new Java4kComYearCategoryListReader(null);
        } catch (IOException e) {
            System.err.println("[ERROR] *** Problem creating the contest reader.  Can't continue.");
            return;
        }
        //ChildProgressController progress = new ChildProgressController(null, 1);
        List<GameDescriptionListResourceReader> descriptionFactories;
        try {
            descriptionFactories = SimpleResourceProducer.load(categoryFactory);
        } catch (Exception e) {
            System.err.println("[ERROR] *** Problem reading the contest entries.  Can't continue.");
            return;
        }
        for (GameDescriptionListResourceReader gdf: descriptionFactories) {
            Thread.sleep(DELAY);
            List<GameDescription> sources;
            try {
                sources = SimpleResourceProducer.load(gdf);
            } catch (Exception e) {
                System.err.println("[ERROR] *** Could not read contest URI " +
                        gdf.getResource() + ": " + e.getMessage());
                continue;
            }
            DefaultGameDetailListResourceReader dgdlrr =
                    new DefaultGameDetailListResourceReader();
            for (GameDescription gd: sources) {
                if (gd != null) {
                    Thread.sleep(DELAY);
                    List<GameDetail> details;
                    try {
                        dgdlrr.addPendingSource(gd);
                        details = SimpleResourceProducer.load(
                                gd.getGameDetailSource(), dgdlrr);
                    } catch (Exception e) {
                        System.err.println("[ERROR] *** Problem loading game " +
                                gd.getName() + " from " + gdf + ": " + e.getMessage());
                        continue;
                    }
                    for (GameDetail detail: details) {
                        GameConfiguration config = null;
                        try {
                            config = detail.createGameConfiguration(tg);
                        } catch (IOException e) {
                            System.err.println("[ERROR] *** Problem reading the configuration for game "
                                    + gd.getName() + " from " + gdf + ": " + e.getMessage());
                        }
                        if (config != null) {
                            try {
                                config.loadCache();
                            } catch (IOException e) {
                                System.err.println("[ERROR] *** Problem loading the files for game " +
                                        gd.getName() + " from " + gdf + ": " + e.getMessage());
                            }
                        }
                    }
                }
            }
        }
    }

}
