package com.muhammaddaffa.mdlib.core.config;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class YamlConfigUpdater {

    private YamlConfigUpdater() {
    }

    @FunctionalInterface
    public interface ResourceOpener {
        /**
         * Opens a resource bundled in the plugin jar, or returns null when it does not exist.
         */
        InputStream open(String path) throws IOException;
    }

    /**
     * Adds every key from the bundled resource that the file on disk is missing, keeping the file's own values,
     * comments and order. A path in {@code ignoredPaths} that already exists in the file is left exactly as it is,
     * including everything below it; an ignored path missing from the file is still added from the resource.
     */
    public static void update(ResourceOpener resources, String resourcePath, File file, Collection<String> ignoredPaths) throws IOException {
        Set<String> ignored = new HashSet<>(ignoredPaths);
        for (String path : ignored) {
            if (path == null || path.isBlank()) {
                throw new IllegalArgumentException("Ignored paths must not be null or blank.");
            }
        }

        String defaults;
        try (InputStream input = resources.open(resourcePath)) {
            if (input == null) {
                throw new IOException("Missing bundled config: " + resourcePath);
            }
            defaults = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        Path destination = file.toPath().toAbsolutePath();
        String original = Files.exists(destination) ? Files.readString(destination) : "";
        String updated;
        try {
            LoaderOptions loader = new LoaderOptions();
            loader.setProcessComments(true);
            loader.setAllowDuplicateKeys(false);
            DumperOptions dumper = new DumperOptions();
            dumper.setProcessComments(true);
            dumper.setSplitLines(false);
            dumper.setLineBreak(original.contains("\r\n") ? DumperOptions.LineBreak.WIN : DumperOptions.LineBreak.UNIX);
            Yaml yaml = new Yaml(new SafeConstructor(loader), new Representer(dumper), dumper, loader);
            MappingNode current = read(yaml, original);
            MappingNode bundled = read(yaml, defaults);
            Map<String, Node> paths = new LinkedHashMap<>();
            index(current, "", paths);
            List<NodeTuple> additions = new ArrayList<>();
            MappingNode merged = merge(current, bundled, "", ignored, paths, additions);
            if (!additions.isEmpty()) {
                List<NodeTuple> values = new ArrayList<>(merged.getValue());
                values.addAll(additions);
                merged = withValues(merged, values);
            }
            if (merged == current) {
                return;
            }
            StringWriter output = new StringWriter();
            yaml.serialize(merged, output);
            updated = output.toString();
            read(yaml, updated);
        } catch (RuntimeException ex) {
            throw new IOException("Cannot update config " + file + ": " + ex.getMessage(), ex);
        }

        Path temporary = Files.createTempFile(destination.getParent(), ".mdlib-config-", ".tmp");
        try {
            Files.writeString(temporary, updated);
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static MappingNode read(Yaml yaml, String contents) {
        Node node = yaml.compose(new StringReader(contents));
        if (node == null) {
            return new MappingNode(Tag.MAP, new ArrayList<>(), DumperOptions.FlowStyle.BLOCK);
        }
        if (node instanceof MappingNode && Tag.COMMENT.equals(node.getTag())) {
            node.setTag(Tag.MAP);
        }
        if (!(node instanceof MappingNode mapping) || !Tag.MAP.equals(mapping.getTag())) {
            throw new IllegalArgumentException("Config must contain a YAML mapping.");
        }
        validate(mapping, new HashSet<>(), new HashSet<>());
        return mapping;
    }

    private static void validate(Node node, Set<Node> visiting, Set<Node> visited) {
        if (!visiting.add(node)) {
            throw new IllegalArgumentException("Recursive YAML aliases are not supported.");
        }
        if (visited.add(node)) {
            if (node instanceof MappingNode mapping) {
                Set<String> keys = new HashSet<>();
                for (NodeTuple entry : mapping.getValue()) {
                    String key = key(entry);
                    if (!keys.add(key)) {
                        throw new IllegalArgumentException("Duplicate YAML key: " + key);
                    }
                    validate(entry.getValueNode(), visiting, visited);
                }
                entries(mapping);
            } else if (node instanceof SequenceNode sequence) {
                for (Node child : sequence.getValue()) {
                    validate(child, visiting, visited);
                }
            }
        }
        visiting.remove(node);
    }

    private static String key(NodeTuple entry) {
        if (!(entry.getKeyNode() instanceof ScalarNode scalar)) {
            throw new IllegalArgumentException("Config keys must be scalar values.");
        }
        if (Tag.STR.equals(scalar.getTag()) || Tag.MERGE.equals(scalar.getTag())) {
            return scalar.getValue();
        }
        return String.valueOf(new KeyConstructor().construct(scalar));
    }

    private static Map<String, NodeTuple> entries(MappingNode mapping) {
        Map<String, NodeTuple> entries = new LinkedHashMap<>();
        for (NodeTuple entry : mapping.getValue()) {
            if (Tag.MERGE.equals(entry.getKeyNode().getTag())) {
                Node value = entry.getValueNode();
                List<Node> sources = value instanceof SequenceNode sequence ? sequence.getValue() : List.of(value);
                for (Node source : sources) {
                    if (!(source instanceof MappingNode inherited)) {
                        throw new IllegalArgumentException("YAML merge sources must be mappings.");
                    }
                    entries(inherited).forEach(entries::putIfAbsent);
                }
            }
        }
        for (NodeTuple entry : mapping.getValue()) {
            if (!Tag.MERGE.equals(entry.getKeyNode().getTag())) {
                entries.put(key(entry), entry);
            }
        }
        return entries;
    }

    private static void index(MappingNode mapping, String parent, Map<String, Node> paths) {
        for (Map.Entry<String, NodeTuple> entry : entries(mapping).entrySet()) {
            String path = parent.isEmpty() ? entry.getKey() : parent + "." + entry.getKey();
            for (int dot = path.indexOf('.'); dot >= 0; dot = path.indexOf('.', dot + 1)) {
                String prefix = path.substring(0, dot);
                if (!(paths.get(prefix) instanceof MappingNode)) {
                    paths.keySet().removeIf(key -> key.startsWith(prefix + "."));
                    paths.put(prefix, new MappingNode(Tag.MAP, new ArrayList<>(), DumperOptions.FlowStyle.BLOCK));
                }
            }
            paths.keySet().removeIf(key -> key.startsWith(path + "."));
            Node value = entry.getValue().getValueNode();
            paths.put(path, value);
            if (value instanceof MappingNode section) {
                index(section, path, paths);
            }
        }
    }

    private static boolean protectedPath(String path, Set<String> ignored, Map<String, Node> paths) {
        for (String ignoredPath : ignored) {
            if (paths.containsKey(ignoredPath)
                    && (path.equals(ignoredPath) || path.startsWith(ignoredPath + "."))) {
                return true;
            }
        }
        for (int dot = path.indexOf('.'); dot >= 0; dot = path.indexOf('.', dot + 1)) {
            Node ancestor = paths.get(path.substring(0, dot));
            if (ancestor != null && !(ancestor instanceof MappingNode)) {
                return true;
            }
        }
        return false;
    }

    private static MappingNode merge(MappingNode current, MappingNode defaults, String parent, Set<String> ignored,
                                     Map<String, Node> paths, List<NodeTuple> additions) {
        Map<String, NodeTuple> existing = entries(current);
        List<NodeTuple> values = new ArrayList<>(current.getValue());
        boolean changed = false;
        for (Map.Entry<String, NodeTuple> entry : entries(defaults).entrySet()) {
            String path = parent.isEmpty() ? entry.getKey() : parent + "." + entry.getKey();
            if (protectedPath(path, ignored, paths)) {
                continue;
            }
            NodeTuple previous = existing.get(entry.getKey());
            Node effective = paths.get(path);
            if (effective == null) {
                values.add(entry.getValue());
                changed = true;
            } else if (effective instanceof MappingNode currentSection
                    && entry.getValue().getValueNode() instanceof MappingNode defaultSection) {
                if (previous == null || previous.getValueNode() != currentSection) {
                    MappingNode empty = new MappingNode(Tag.MAP, new ArrayList<>(), DumperOptions.FlowStyle.BLOCK);
                    MappingNode missing = merge(empty, defaultSection, path, ignored, paths, additions);
                    for (NodeTuple child : missing.getValue()) {
                        ScalarNode originalKey = (ScalarNode) child.getKeyNode();
                        ScalarNode dottedKey = new ScalarNode(Tag.STR, path + "." + key(child),
                                null, null, originalKey.getScalarStyle());
                        dottedKey.setBlockComments(originalKey.getBlockComments());
                        dottedKey.setInLineComments(originalKey.getInLineComments());
                        dottedKey.setEndComments(originalKey.getEndComments());
                        additions.add(new NodeTuple(dottedKey, child.getValueNode()));
                    }
                    continue;
                }
                MappingNode section = merge(currentSection, defaultSection, path, ignored, paths, additions);
                if (section != currentSection) {
                    NodeTuple replacement = new NodeTuple(previous.getKeyNode(), section);
                    int index = values.indexOf(previous);
                    if (index < 0) {
                        values.add(replacement);
                    } else {
                        values.set(index, replacement);
                    }
                    changed = true;
                }
            }
        }
        if (!changed) {
            return current;
        }
        return withValues(current, values);
    }

    private static MappingNode withValues(MappingNode current, List<NodeTuple> values) {
        MappingNode result = new MappingNode(current.getTag(), values, current.getFlowStyle());
        result.setBlockComments(current.getBlockComments());
        result.setInLineComments(current.getInLineComments());
        result.setEndComments(current.getEndComments());
        return result;
    }
    private static final class KeyConstructor extends SafeConstructor {

        private KeyConstructor() {
            super(new LoaderOptions());
        }

        // Resolves non-string scalar keys (1, on, true) the way Bukkit's YamlConstructor does, so equivalent keys match.
        private Object construct(Node node) {
            return constructObject(node);
        }

    }
}
