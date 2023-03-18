package ch.njol.minecraft.config;

import ch.njol.minecraft.config.annotations.Category;
import ch.njol.minecraft.config.annotations.Dropdown;
import java.lang.reflect.Field;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Supplier;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.gui.entries.SubCategoryListEntry;
import me.shedaniel.clothconfig2.impl.ConfigEntryBuilderImpl;
import net.minecraft.client.gui.ParentElement;
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

	private final HashMap<Subcategory, SubCategoryListEntry> subCategories = new HashMap<>();

	protected ConfigScreen(Screen parent, String translateRoot, Supplier<T> optionsSupplier, T defaultOptions) {
		super(new TranslatableText(translateRoot + ".title"));
		this.parent = parent;
		this.translateRoot = translateRoot;
		this.optionsSupplier = optionsSupplier;
		this.defaultOptions = defaultOptions;
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected void init() {
		ConfigBuilder config = ConfigBuilder.create()
			                       .setParentScreen(parent)
			                       .setTitle(new TranslatableText(translateRoot + ".title"));

		T options = optionsSupplier.get();

		for (Field field : options.getClass().getDeclaredFields()) {
			Category categoryAnnotation = field.getAnnotation(Category.class);
			Dropdown dropdownAnnotation = field.getAnnotation(Dropdown.class);
			if (categoryAnnotation == null || !options.categoryVisible(categoryAnnotation.value())) {
				continue;
			}
			ConfigCategory category = config.getOrCreateCategory(new TranslatableText(translateRoot + ".category." + categoryAnnotation.value()));

			if (dropdownAnnotation != null) {
				Subcategory subcategory = new Subcategory(dropdownAnnotation.value(), categoryAnnotation);

				if (subCategories.containsKey(subcategory)) {
					AbstractConfigListEntry entry = ClothConfigSetup.buildConfigEntry(options, defaultOptions, field, translateRoot + ".option");
					subCategories.get(subcategory).getValue().add(entry);
					//DO NOT REMOVE THE SETEXPANDED, APPARENTLY YOU CAN'T ACCESS THE CHILDRENS WITHOUT IT >:(
					subCategories.get(subcategory).setExpanded(true);
					((AbstractList<ParentElement>) subCategories.get(subcategory).children()).add(entry);
				} else {
					ArrayList<AbstractConfigListEntry> list = new ArrayList<>();
					list.add(ClothConfigSetup.buildConfigEntry(options, defaultOptions, field, translateRoot + ".option"));
					SubCategoryListEntry entry = ConfigEntryBuilderImpl.create().startSubCategory(new TranslatableText(translateRoot + ".subcategory." + dropdownAnnotation.value()), list).build();
					subCategories.put(subcategory, entry);
					category.addEntry(entry);
				}
				continue;
			}
			category.addEntry(ClothConfigSetup.buildConfigEntry(options, defaultOptions, field, translateRoot + ".option"));
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

	public record Subcategory(String name, Category category) {
	}

}
