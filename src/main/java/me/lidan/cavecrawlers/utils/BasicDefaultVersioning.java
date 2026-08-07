package me.lidan.cavecrawlers.utils;

import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import org.jspecify.annotations.NonNull;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class BasicDefaultVersioning extends BasicVersioning {
    private final String route;
    private final int lastVersion;

    /**
     * Creates a versioning with the given route.
     *
     * @param route the route at which version IDs can be found
     */
    public BasicDefaultVersioning(@NonNull String route) {
        this(route, 1);
    }

    /**
     * Creates a versioning with the given route.
     *
     * @param route the route at which version IDs can be found
     * @param lastVersion the last version for virtual default
     */
    public BasicDefaultVersioning(@NonNull String route, int lastVersion) {
        super(route);
        this.route = route;
        this.lastVersion = lastVersion;
    }

    public ByteArrayInputStream getVirtualDefaults() {
        return new ByteArrayInputStream((route + ": " + lastVersion).getBytes(StandardCharsets.UTF_8));
    }
}
