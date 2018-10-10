package de.photon.AACAdditionPro.util.files;

import java.io.File;
import java.io.IOException;

public final class FileUtil
{
    /**
     * Creates a new {@link File} and all missing parent directories.
     *
     * @param path the path of the {@link File} which should be created.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static File createFile(final String path) throws IOException
    {
        return createFile(new File(path));
    }

    /**
     * Creates a new {@link File} and all missing parent directories.
     *
     * @param file the {@link File} which should be created.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static File createFile(final File file) throws IOException
    {
        file.getParentFile().mkdirs();
        file.createNewFile();
        return file;
    }
}
