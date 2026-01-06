package me.lidan.cavecrawlers.utils;

import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import org.jspecify.annotations.NonNull;

import java.io.ByteArrayInputStream;

public class BasicDefaultVersioning extends BasicVersioning {
    private final String route;

    /**
     * Creates a versioning with the given route.
     *
     * @param route the route at which version IDs can be found
     */
    public BasicDefaultVersioning(@NonNull String route) {
        super(route);
        this.route = route;
    }

    public ByteArrayInputStream getVirtualDefaults() {
        String id = getFirstVersion().asID();
        return new ByteArrayInputStream((route + ": " + id).getBytes());
    }
}
