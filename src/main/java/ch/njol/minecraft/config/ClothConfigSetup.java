package ch.njol.minecraft.config;

import ch.njol.minecraft.config.annotations.DescriptionLine;
import ch.njol.minecraft.config.annotations.FloatSlider;
import ch.njol.minecraft.config.annotations.IntSlider;
import ch.njol.minecraft.config.annotations.Regex;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.PatternSyntaxException;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.impl.ConfigEntryBuilderImpl;
import me.shedaniel.clothconfig2.impl.builders.TextFieldBuilder;
import me.shedaniel.math.Color;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

/*
 * All methods that rely on cloth config (and optionally mod menu) being present must go here.
 */
public class ClothConfigSetup {

	public interface ConfigEntryBuilder<T> {
		AbstractConfigListEntry<?> buildConfigEntry(T value, T defaultValue, Field field, String translatePath, Consumer<T> saveConsumer);
	}

	private static final Map<Class<?>, ConfigEntryBuilder<?>> builders = new HashMap<>();
	private static final Map<Class<?>, ConfigEntryBuilder<?>> superBuilders = new HashMap<>();

	public static <T> void registerType(Class<T> type, ConfigEntryBuilder<T> configEntryBuilder) {
		builders.put(type, configEntryBuilder);
	}

	public static <T> void registerSuperType(Class<T> type, ConfigEntryBuilder<T> configEntryBuilder) {
		superBuilders.put(type, configEntryBuilder);
	}

	static {
		// This code cannot be simplified well because ClothConfig sucks. E.g. setTooltip is defined separately for every subclass...
		registerType(DescriptionLine.class, (value, defaultValue, field, translatePath, saveConsumer) ->
			                                    ConfigEntryBuilderImpl.create()
				                                    .startTextDescription(Text.translatable(translatePath))
				                                    .build());
		registerType(Boolean.TYPE, (value, defaultValue, field, translatePath, saveConsumer) ->
			                           ConfigEntryBuilderImpl.create()
				                           .startBooleanToggle(Text.translatable(translatePath), value)
				                           .setDefaultValue(defaultValue)
				                           .setTooltip(Text.translatable(translatePath + ".tooltip"))
				                           .setSaveConsumer(saveConsumer)
				                           .build());
		registerType(Integer.TYPE, (value, defaultValue, field, translatePath, saveConsumer) -> {
			Text text = Text.translatable(translatePath);
			Text tooltip = Text.translatable(translatePath + ".tooltip");
			IntSlider slider = field.getAnnotation(IntSlider.class);
			if (field.getAnnotation(ch.njol.minecraft.config.annotations.Color.class) != null) {
				return ConfigEntryBuilderImpl.create()
					       .startColorField(text, Color.ofOpaque(value))
					       .setDefaultValue(defaultValue)
					       .setTooltip(tooltip)
					       .setSaveConsumer(saveConsumer)
					       .build();
			} else if (slider != null) {
				return ConfigEntryBuilderImpl.create()
					       .startIntSlider(text, value, slider.min(), slider.max())
					       .setDefaultValue(defaultValue)
					       .setTextGetter(v -> MutableText.of(!slider.minText().isEmpty() && v <= slider.min() ? Text.translatable(slider.minText()).getContent()
						                                          : !slider.maxText().isEmpty() && v >= slider.max() ? Text.translatable(slider.maxText()).getContent()
							                                            : new LiteralTextContent(v + slider.unit())))
					       .setTooltip(tooltip)
					       .setSaveConsumer(saveConsumer)
					       .build();
			} else {
				return ConfigEntryBuilderImpl.create()
					       .startIntField(text, value)
					       .setDefaultValue(defaultValue)
					       .setTooltip(tooltip)
					       .setSaveConsumer(saveConsumer)
					       .build();
			}
		});
		DecimalFormat numberFormat = new DecimalFormat("0.##");
		registerType(Float.TYPE, (value, defaultValue, field, translatePath, saveConsumer) -> {
			Text text = Text.translatable(translatePath);
			Text tooltip = Text.translatable(translatePath + ".tooltip");
			FloatSlider slider = field.getAnnotation(FloatSlider.class);
			if (slider != null) {
				float step = slider.step();
				return ConfigEntryBuilderImpl.create()
					       .startLongSlider(text, Math.round(value / step), Math.round(slider.min() / step), Math.round(slider.max() / step))
					       .setDefaultValue(Math.round(defaultValue / slider.step()))
					       .setTextGetter(l -> MutableText.of(!slider.minText().isEmpty() && l * step - 0.01 * step <= slider.min() ? Text.translatable(slider.minText()).getContent()
						                                          : !slider.maxText().isEmpty() && l * step + 0.01 * step >= slider.max() ? Text.translatable(slider.maxText()).getContent()
							                                            : new LiteralTextContent(numberFormat.format(l * step * slider.unitStep()) + slider.unit())))
					       .setTooltip(tooltip)
					       .setSaveConsumer(l -> saveConsumer.accept(l * step))
					       .build();
			} else {
				return ConfigEntryBuilderImpl.create()
					       .startFloatField(text, value)
					       .setDefaultValue(defaultValue)
					       .setTooltip(tooltip)
					       .setSaveConsumer(saveConsumer)
					       .build();
			}
		});
		registerType(String.class, (value, defaultValue, field, translatePath, saveConsumer) -> {
			TextFieldBuilder builder = ConfigEntryBuilderImpl.create()
				                           .startTextField(Text.translatable(translatePath), value)
				                           .setDefaultValue(defaultValue)
				                           .setTooltip(Text.translatable(translatePath + ".tooltip"))
				                           .setSaveConsumer(saveConsumer);
			if (field.getAnnotation(Regex.class) != null) {
				builder.setErrorSupplier(s -> {
					try {
						Config.toPattern(s);
						return Optional.empty();
					} catch (PatternSyntaxException e) {
						return Optional.of(Text.of(e.getMessage()));
					}
				});
			}
			return builder.build();
		});
		registerSuperType(Enum.class, (value, defaultValue, field, translatePath, saveConsumer) ->
			                              ConfigEntryBuilderImpl.create()
				                              .startEnumSelector(Text.translatable(translatePath), (Class<Enum<?>>) field.getType(), (Enum<?>) value)
				                              .setDefaultValue((Enum<?>) defaultValue)
				                              .setTooltip(Text.translatable(translatePath + ".tooltip"))
				                              .setEnumNameProvider(v -> Text.translatable("njols-config-framework.enum." + v.getDeclaringClass().getSimpleName() + "." + v.name()))
				                              .setSaveConsumer(saveConsumer::accept)
				                              .build());
	}

	public static AbstractConfigListEntry<?> buildConfigEntry(Object object, Object defaultObject, Field field, String parentTranslatePath) {
		return buildConfigEntryT(object, defaultObject, field, parentTranslatePath);
	}

	// Suppress warnings: lots of casts to T, which is the type of the field, and thus also the generic type parameter of the builder
	@SuppressWarnings("unchecked")
	private static <T> AbstractConfigListEntry<?> buildConfigEntryT(Object object, Object defaultObject, Field field, String parentTranslatePath) {
		try {
			T value = (T) field.get(object);
			T defaultValue = (T) field.get(defaultObject);
			Consumer<T> saveConsumer = val -> {
				try {
					field.set(object, val);
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			};
			Class<?> type = field.getType();
			ConfigEntryBuilder<T> builder = (ConfigEntryBuilder<T>) builders.get(type);
			if (builder == null) {
				// this is actually a <? super T>, but Java doesn't know that a Consumer<? super T> is a supertype of Consumer<T> because it has no variance
				builder = (ConfigEntryBuilder<T>) superBuilders.entrySet().stream()
					                                  .filter(e -> e.getKey().isAssignableFrom(type))
					                                  .findFirst()
					                                  .orElseThrow(() -> new RuntimeException("Unexpected field type in " + object.getClass() + ": " + field))
					                                  .getValue();
			}
			return builder.buildConfigEntry(value, defaultValue, field, parentTranslatePath + "." + field.getName(), saveConsumer);

		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

}
