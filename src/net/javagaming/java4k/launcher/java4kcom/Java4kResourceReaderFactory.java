package net.javagaming.java4k.launcher.java4kcom;

import net.javagaming.java4k.launcher.CategoryListResourceReader;
import net.javagaming.java4k.launcher.LauncherManager;

import java.io.IOException;

/**
 * @author Groboclown
 */
public class Java4kResourceReaderFactory {

    public CategoryListResourceReader createCategoryListResourceReader(
            LauncherManager launcherManager)
            throws IOException {
        return new Java4kComYearCategoryListReader(launcherManager);
    }


}
