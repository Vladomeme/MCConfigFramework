package ch.njol.minecraft.config;

import ch.njol.minecraft.config.annotations.Category;
import java.lang.reflect.Field;
import java.util.function.Supplier;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.TranslatableText;

/**
 * This fake screen is a workaround for a bug when using both modmenu and cloth-config - cloth config expects a new screen each time, but modmenu caches the screen.
 */
public class ConfigScreen<T extends Options> extends Screen {

	private final Screen parent;
	private final String translateRoot;
	private final Supplier<T> optionsSupplier;
	private final T defaultOptions;

	protected ConfigScreen(Screen parent, String translateRoot, Supplier<T> optionsSupplier, T defaultOptions) {
		super(new TranslatableText(translateRoot + ".title"));
		this.parent = parent;
		this.translateRoot = translateRoot;
		this.optionsSupplier = optionsSupplier;
		this.defaultOptions = defaultOptions;
	}

	@Override
	protected void init() {
		ConfigBuilder config = ConfigBuilder.create()
			                       .setParentScreen(parent)
			                       .setTitle(new TranslatableText(translateRoot + ".title"));

		T options = optionsSupplier.get();

		for (Field field : options.getClass().getDeclaredFields()) {
			Category categoryAnnotation = field.getAnnotation(Category.class);
			if (categoryAnnotation == null || !options.categoryVisible(categoryAnnotation.value())) {
				continue;
			}
			ConfigCategory category = config.getOrCreateCategory(new TranslatableText(translateRoot + ".category." + categoryAnnotation.value()));
			category.addEntry(Config.buildConfigEntry(options, defaultOptions, field, translateRoot + ".option"));
		}

		config.setSavingRunnable(options::onUpdate);

		if (client != null) {
			client.setScreen(config.build());
		}
	}

	@Override
	public void close() {
		if (client != null) {
			client.setScreen(parent);
		}
	}

}
