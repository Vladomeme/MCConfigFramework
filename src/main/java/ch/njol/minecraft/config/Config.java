package ch.njol.minecraft.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.fabricmc.loader.api.FabricLoader;

public class Config {

	public static Pattern toPattern(String s) throws PatternSyntaxException {
		return s == null || s.isEmpty() ? null : Pattern.compile(s);
	}

	public static <T> T readJsonFile(Class<T> c, String filePath) throws IOException, JsonParseException {
		return readJsonFile(new GsonBuilder().create(), c, filePath);
	}

	public static void writeJsonFile(Object o, String filePath) throws IOException {
		writeJsonFile(new GsonBuilder().setPrettyPrinting().create(), o, filePath);
	}

	public static <T> T readJsonFile(Gson gson, Class<T> c, String filePath) throws IOException, JsonParseException {
		try (FileReader reader = new FileReader(FabricLoader.getInstance().getConfigDir().resolve(filePath).toFile())) {
			return gson.fromJson(reader, c);
		}
	}

	public static void writeJsonFile(Gson gson, Object o, String filePath) throws IOException {
		try (FileWriter writer = new FileWriter((FabricLoader.getInstance().getConfigDir().resolve(filePath).toFile()))) {
			writer.write(gson.toJson(o));
		}
	}

}
