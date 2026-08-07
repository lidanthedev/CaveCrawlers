package me.lidan.cavecrawlers.objects;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.route.Route;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings.Builder;
import dev.dejvokep.boostedyaml.settings.updater.ValueMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.lidan.cavecrawlers.utils.BasicDefaultVersioning;
import org.jspecify.annotations.NonNull;

import java.util.Set;
import java.util.function.Consumer;

@Slf4j
public class BoostedUpdaterSettings {
    private final UpdaterSettings.Builder updateBuilder;
    @Getter
    private int lastVersion = 1;

    public BoostedUpdaterSettings(Builder updateBuilder) {
        this.updateBuilder = updateBuilder;
    }

    private static int tryParseInt(String text, int defaultValue) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public BoostedUpdaterSettings addCustomLogic(@NonNull String versionId, @NonNull Consumer<YamlDocument> consumer) {
        updateLastVersionIfNeeded(versionId);
        updateBuilder.addCustomLogic(versionId, consumer);
        return this;
    }

    public BoostedUpdaterSettings addIgnoredRoutes(@NonNull String versionId, @NonNull Set<Route> routes) {
        updateLastVersionIfNeeded(versionId);
        updateBuilder.addIgnoredRoutes(versionId, routes);
        return this;
    }

    public BoostedUpdaterSettings addMapper(@NonNull String versionId, @NonNull Route route, @NonNull ValueMapper mapper) {
        updateLastVersionIfNeeded(versionId);
        updateBuilder.addMapper(versionId, route, mapper);
        return this;
    }

    public BoostedUpdaterSettings addRelocation(@NonNull String versionId, @NonNull Route fromRoute, @NonNull Route toRoute) {
        updateLastVersionIfNeeded(versionId);
        updateBuilder.addRelocation(versionId, fromRoute, toRoute);
        return this;
    }

    public UpdaterSettings build() {
        updateBuilder.setVersioning(new BasicDefaultVersioning(ConfigLoader.VERSION_KEY, lastVersion));
        return updateBuilder.build();
    }

    private void updateLastVersionIfNeeded(String versionId) {
        int version = tryParseInt(versionId, 1);
        if (version > lastVersion) {
            lastVersion = version;
        }
    }
}
