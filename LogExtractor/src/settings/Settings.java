package settings;

public class Settings {
	public static String getWorkspaceProjectName() {
		return "android_src";
	}

	public static String[] getLoggerClassNames() {
		String[] loggerClassNames = {"Rlog","Slog","Log"}; 
		return loggerClassNames;
	}

	public static String getLogFramework() {
		return "android";
	}
}
