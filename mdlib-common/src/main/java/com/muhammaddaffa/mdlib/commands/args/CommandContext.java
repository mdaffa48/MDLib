package com.muhammaddaffa.mdlib.commands.args;

import java.util.HashMap;
import java.util.Map;

public final class CommandContext {
    private final Map<String, Object> values = new HashMap<>();

    public <T> void put(String name, T value) {
        values.put(name, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String name, Class<T> type) {
        Object v = values.get(name);
        if (v == null) return null;
        if (!type.isInstance(v)) {
            throw new IllegalStateException("Argument '" + name + "' is not a " + type.getSimpleName());
        }
        return (T) v;
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(String name, Class<T> type, T def) {
        T v = get(name, type);
        return v == null ? def : v;
    }

    public boolean has(String name) { return values.containsKey(name); }
}
