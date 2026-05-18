package net.marewmod.quiver.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.marewmod.quiver.config.QuiverConfig;
import net.marewmod.quiver.config.QuiverConfigScreen;

public class QuiverModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new QuiverConfigScreen(parent);
    }
}
