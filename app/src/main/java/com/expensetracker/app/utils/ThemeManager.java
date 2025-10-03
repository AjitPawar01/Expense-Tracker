package com.expensetracker.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {
    private static final String THEME_PREFERENCE = "theme_preference";
    private static final String THEME_KEY = "selected_theme";

    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;
    public static final int THEME_SYSTEM = 2;

    private static SharedPreferences getThemePreferences(Context context) {
        return context.getSharedPreferences(THEME_PREFERENCE, Context.MODE_PRIVATE);
    }

    public static void applyTheme(int themeMode) {
        switch (themeMode) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case THEME_SYSTEM:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    public static void saveTheme(Context context, int themeMode) {
        SharedPreferences.Editor editor = getThemePreferences(context).edit();
        editor.putInt(THEME_KEY, themeMode);
        editor.apply();
        applyTheme(themeMode);
    }

    public static int getSavedTheme(Context context) {
        return getThemePreferences(context).getInt(THEME_KEY, THEME_SYSTEM);
    }

    public static void initializeTheme(Context context) {
        int savedTheme = getSavedTheme(context);
        applyTheme(savedTheme);
    }

    public static String getThemeName(int themeMode) {
        switch (themeMode) {
            case THEME_LIGHT:
                return "Light";
            case THEME_DARK:
                return "Dark";
            case THEME_SYSTEM:
                return "System Default";
            default:
                return "Unknown";
        }
    }
}