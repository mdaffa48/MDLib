package com.muhammaddaffa.mdlib.commands.args;

import java.util.HashMap;
import java.util.Map;

public class PartialContext {

    private final Map<String, Object> values = new HashMap<>();

    public <T> void put(String name, T value) { values.put(name, value); }

    @SuppressWarnings("unchecked")
    public <T> T get(String name, Class<T> type) {
        Object v = values.get(name);
        if (v == null) return null;
        if (!type.isInstance(v)) return null;
        return (T) v;
    }

    public boolean has(String name) { return values.containsKey(name); }

}
