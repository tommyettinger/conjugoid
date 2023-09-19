package com.github.tommyettinger.conjugoid;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.github.tommyettinger.conjugoid.annotations.Beta;

import java.io.IOException;

/**
 * Various utility methods that are still being sorted out; this class should be deleted once the code is in its place.
 */
@Beta
public final class BetaTools {
    /**
     * Not instantiable.
     */
    private BetaTools() {
    }

    /**
     * An alternative to {@link FileHandle#exists()} that also checks whether a file exists (and is not a directory),
     * but does so much more quickly on Android and avoids an infrequent, unpredictable warning on iOS. This isn't part
     * of libGDX proper presumably because it (internally) uses Exceptions for control flow. There isn't any
     * particularly great solution on Android to check if a file exists in the internal assets.
     *
     * @see <a href="https://github.com/libgdx/libgdx/issues/2342">libGDX issue about Android's FileHandle.exists()</a>
     * @see <a href="https://github.com/libgdx/libgdx/issues/2345">libGDX issue about the iOS warning</a>
     * @param fh a FileHandle that might represent a file
     * @return true if {@code fh} represents an extant File and not a directory; false otherwise
     */
    public static boolean fileExists(FileHandle fh) {
        try {
            fh.read().close();
            return true;
        } catch (GdxRuntimeException | IOException | NullPointerException ignored) {
            return false;
        }
    }
}
