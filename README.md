[![](https://jitpack.io/v/mdaffa48/MDLib.svg)](https://jitpack.io/#mdaffa48/MDLib)
```xml
<repository>
  <id>jitpack.io</id>
  <url>https://jitpack.io</url>
</repository>

<dependency>
  <groupId>com.github.mdaffa48</groupId>
  <artifactId>MDLib</artifactId>
  <version>Tag</version>
</dependency>
```

# MiniMessage messages

`Config.sendMessage(sender, path)` supports single strings and YAML lists. Messages
are sent as Adventure components, preserving MiniMessage click, hover, font,
translation, and formatting tags supported by the server's Adventure version.

```yaml
messages:
  help: '<click:run_command:/help><hover:show_text:"<yellow>Click for help"><green>Help</green></hover></click>'
  welcome: '<green>Hello, %player_name%!</green>'
  notification: 'actionbar;<gold>Saved!</gold>'
```

`Common.sendMessage` parses MDLib placeholders first, then PlaceholderAPI for player
senders when that plugin is enabled, then MiniMessage. Console messages skip PAPI.
Direct action bars and titles also parse PAPI with the receiving player. Broadcasts
have no individual player context. Placeholder values are treated as trusted
formatting input; escape untrusted input or use an unparsed MiniMessage resolver.

`Common.component(text)` and `ColorComponent.colorToComponent(text)` preserve
components. `Common.color(text)` remains a legacy string compatibility API and
cannot retain click/hover events. Legacy `&`/section-sign colors
and hex colors remain supported in ordinary text. Quoted tag arguments are passed
unchanged to MiniMessage; use MiniMessage formatting inside hover text.

Custom tags can be registered once at plugin startup; they then work through
`Config.sendMessage`, `Common.component`, and `ColorComponent`:

```java
Common.registerTagResolver("my-plugin", myTagResolver);
// Remove when disabling the plugin:
Common.unregisterTagResolver("my-plugin");
```

For a single parse, use `Common.component(text, resolver)`. Per-parse resolvers take
precedence over registered resolvers and standard tags.

# Command Example
```java
public final class KitCommand extends RoutedCommand {
    public KitCommand() {
        // name, description, usage, permission
        super("kit", "Manage kits", "/kit <give|reload> | /kit <name>", "yourplugin.kit");

        // root level aliases
        alias("kitkit", "k");
        
        root() // `/kit <name>`
          .arg("name", new StringArg())
          .exec((sender, ctx) -> { /* apply kit */ return true; });

        sub("give") // `/kit give <player> <name> [silent]`
          .alias("g", "grant")    // sub-level aliases   
          .perm("yourplugin.kit.give")
          .arg("player", new OnlinePlayerArg())
          .arg("name", new StringArg())
          .argOptional("silent", new BoolArg())
          .exec((sender, ctx) -> { /* give kit to target */ return true; });

        sub("reload") // `/kit reload`
          .perm("yourplugin.kit.admin")
          .exec((sender, ctx) -> { sender.sendMessage("§aKits reloaded."); return true; });
    }
}
```
