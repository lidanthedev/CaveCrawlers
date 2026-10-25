package me.lidan.cavecrawlers.stats;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AbsorptionAccessorResolverTest {
    @Test
    void resolvesNamedMetadataWithoutAssumingAnIndex() {
        assertSame(MappedPlayer.DATA_PLAYER_ABSORPTION_ID, resolve(MappedPlayer.class));
    }

    @Test
    void resolvesObfuscatedMetadataByItsFloatSerializer() {
        assertSame(ObfuscatedPlayer.bN, resolve(ObfuscatedPlayer.class));
    }

    @Test
    void supportsAccessorsMovedToAPlayerSuperclass() {
        assertSame(ObfuscatedPlayer.bN, resolve(NewerPlayer.class));
    }

    @Test
    void prefersKnownAbsorptionWhenFuturePlayerClassesAddAnotherFloat() {
        assertSame(MappedPlayer.DATA_PLAYER_ABSORPTION_ID, resolve(AdditionalFloatPlayer.class));
    }

    @Test
    void refusesToMistakeLivingEntityHealthForMissingAbsorption() {
        assertThrows(IllegalStateException.class, () -> resolve(MissingPlayer.class));
    }

    @Test
    void refusesAmbiguousObfuscatedMetadata() {
        assertThrows(IllegalStateException.class, () -> resolve(AmbiguousPlayer.class));
    }

    @Test
    void ignoresOrdinaryFloatFieldsInstanceAccessorsAndNonFloatSerializers() {
        assertSame(ObfuscatedPlayer.bN, resolve(NoisePlayer.class));
        assertThrows(IllegalStateException.class, () -> resolve(WrongSerializerPlayer.class));
    }

    @Test
    void rejectsAnUnrecognizedEntityHierarchy() {
        assertThrows(IllegalStateException.class, () -> resolve(String.class));
    }

    private static Object resolve(Class<?> player) {
        return AbsorptionAccessorResolver.resolve(player, Living.class, Key.class,
                value -> ((Key) value).serializer == Float.class);
    }

    private record Key(Class<?> serializer) {
    }

    private static class Living {
        private static final Key HEALTH = new Key(Float.class);
    }

    private static class MappedPlayer extends Living {
        private static final Key DATA_PLAYER_ABSORPTION_ID = new Key(Float.class);
        private static final Key SCORE = new Key(Integer.class);
    }

    private static class ObfuscatedPlayer extends Living {
        private static final Key bN = new Key(Float.class);
        private static final Key bO = new Key(Integer.class);
    }

    private static class NewerPlayer extends ObfuscatedPlayer {
    }

    private static class AdditionalFloatPlayer extends MappedPlayer {
        private static final Key OTHER = new Key(Float.class);
    }

    private static class MissingPlayer extends Living {
    }

    private static class AmbiguousPlayer extends ObfuscatedPlayer {
        private static final Key c = new Key(Float.class);
    }

    private static class NoisePlayer extends ObfuscatedPlayer {
        private static final float UNRELATED = 3;
        private final Key instanceField = new Key(Float.class);
        private static final Key FLAG = new Key(Byte.class);
    }

    private static class WrongSerializerPlayer extends Living {
        private static final Key DATA_PLAYER_ABSORPTION_ID = new Key(Integer.class);
    }
}
