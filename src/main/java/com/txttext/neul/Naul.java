package com.txttext.neul;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Naul implements ModInitializer {

    @Override
    public void onInitialize() {
        Logger Logger = LogManager.getLogger();
        Logger.info("Limitless Anvil success to load.");
    }
}
