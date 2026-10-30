package com.muhammaddaffa.mdlib.core.config;

import java.io.File;
import java.util.List;

public interface ConfigFile {

    String getConfigName();

    File getFile();

    boolean isShouldReload();

    boolean isShouldUpdate();

    void setShouldUpdate(boolean shouldUpdate);

    void addIgnored(String ignoredPath);

    String getString(String path);

    String getString(String path, String defaultValue);

    List<String> getStringList(String path);

    int getInt(String path);

    int getInt(String path, int defaultValue);

    List<Integer> getIntegerList(String path);

    double getDouble(String path);

    double getDouble(String path, double defaultValue);

    boolean getBoolean(String path);

    long getLong(String path);

    boolean isConfigurationSection(String path);

    void saveConfig();

    void reloadConfig();

}
