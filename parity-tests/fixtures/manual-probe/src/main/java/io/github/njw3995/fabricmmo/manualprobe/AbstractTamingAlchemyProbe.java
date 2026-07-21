package io.github.njw3995.fabricmmo.manualprobe;

import io.github.njw3995.fabricmmo.api.FabricMmoApi;
import io.github.njw3995.fabricmmo.api.entrypoint.FabricMmoEntrypoint;
import io.github.njw3995.fabricmmo.api.event.AlchemyBrewEvent;
import io.github.njw3995.fabricmmo.api.event.AlchemyCatalysisEvent;
import io.github.njw3995.fabricmmo.api.event.TamingEntityTamedEvent;
import io.github.njw3995.fabricmmo.api.event.TamingSummonEvent;
import java.util.Locale;

abstract class AbstractTamingAlchemyProbe implements FabricMmoEntrypoint {
    private static final String PREFIX = "[FabricMMO Taming/Alchemy Probe] ";
    private final Mode mode;

    AbstractTamingAlchemyProbe(Mode mode) {
        this.mode = mode;
    }

    @Override
    public final void register(FabricMmoApi api) {
        log("Loaded mode=" + mode.name().toLowerCase(Locale.ROOT));
        api.events().subscribe(TamingEntityTamedEvent.class, this::onTamed);
        api.events().subscribe(TamingSummonEvent.class, this::onSummon);
        api.events().subscribe(AlchemyCatalysisEvent.class, this::onCatalysis);
        api.events().subscribe(AlchemyBrewEvent.class, this::onBrew);
    }

    private void onTamed(TamingEntityTamedEvent event) {
        double originalXp = event.xp();
        if (mode == Mode.CANCEL) {
            event.cancel();
        } else if (mode == Mode.MUTATE) {
            event.setXp(originalXp * 2.0D);
        }
        log("TamingEntityTamedEvent player=" + event.playerId()
                + " entity=" + event.entityId()
                + " type=" + event.entityType()
                + " originalXp=" + originalXp
                + " finalXp=" + event.xp()
                + " cancelled=" + event.cancelled());
    }

    private void onSummon(TamingSummonEvent event) {
        if (mode == Mode.CANCEL) {
            event.cancel();
        }
        log("TamingSummonEvent player=" + event.playerId()
                + " entity=" + event.entityId()
                + " type=" + event.summonType()
                + " lifetime=" + event.lifetime()
                + " cancelled=" + event.cancelled());
    }

    private void onCatalysis(AlchemyCatalysisEvent event) {
        double originalSpeed = event.speed();
        if (mode == Mode.CANCEL) {
            event.cancel();
        } else if (mode == Mode.MUTATE) {
            event.setSpeed(originalSpeed * 2.0D);
        }
        log("AlchemyCatalysisEvent player=" + event.playerId()
                + " dimension=" + event.dimensionId()
                + " blockPosition=" + event.blockPosition()
                + " originalSpeed=" + originalSpeed
                + " finalSpeed=" + event.speed()
                + " cancelled=" + event.cancelled());
    }

    private void onBrew(AlchemyBrewEvent event) {
        if (mode == Mode.CANCEL) {
            event.cancel();
        }
        log("AlchemyBrewEvent player=" + event.playerId()
                + " dimension=" + event.dimensionId()
                + " blockPosition=" + event.blockPosition()
                + " ingredient=" + event.ingredientId()
                + " outputs=" + event.outputPotionIds()
                + " cancelled=" + event.cancelled());
    }

    private static void log(String message) {
        System.out.println(PREFIX + message);
    }

    enum Mode {
        OBSERVE,
        CANCEL,
        MUTATE
    }
}
