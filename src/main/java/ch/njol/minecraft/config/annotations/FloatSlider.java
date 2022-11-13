package ch.njol.minecraft.config.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface FloatSlider {
	float min();

	float max();

	float step();

	/**
	 * Unit to display, e.g. "%" or " blocks"
	 */
	String unit() default "";

	/**
	 * Multiplier applied before displaying the raw value (e.g. 100 for %)
	 */
	float unitStep() default 1f;

	String minText() default "";

	String maxText() default "";

}
