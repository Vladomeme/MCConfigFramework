package ch.njol.minecraft.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import java.util.function.Supplier;

/*
 * All methods that rely on mod menu being present must go here.
 */
public class ModMenuConfigSetup {

	public static <T extends Options> ConfigScreenFactory<?> getModConfigScreenFactory(String translateRoot, Supplier<T> options, T defaultOptions) {
		try {
			Class.forName("me.shedaniel.clothconfig2.api.ConfigBuilder");
		} catch (ClassNotFoundException e) {
			return parent -> null;
		}
		return parent -> new ConfigScreen<>(parent, translateRoot, options, defaultOptions);
	}

}
