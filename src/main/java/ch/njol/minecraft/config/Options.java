package ch.njol.minecraft.config;

public interface Options {

	void onUpdate();

	default boolean categoryVisible(String category) {
		return true;
	}

	/*
		old code for Regexes from other mod:
		for (Field field : Options.class.getDeclaredFields()) {
			if (field.getAnnotation(Regex.class) != null) {
				try {
					String s = (String) field.get(this);
					Pattern pattern;
					try {
						pattern = toPattern(s);
					} catch (PatternSyntaxException e) {
						pattern = null;
					}
					Field patternField = Options.class.getDeclaredField(field.getName() + "Pattern");
					patternField.set(this, pattern);
				} catch (final IllegalAccessException | NoSuchFieldException e) {
					e.printStackTrace();
				}
			}
		}
	*/
}
