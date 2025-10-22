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

# Command Example
```java
public final class KitCommand extends RoutedCommand {
    public KitCommand() {
        // name, description, usage, permission
        super("kit", "Manage kits", "/kit <give|reload> | /kit <name>", "yourplugin.kit");

        root() // `/kit <name>`
          .arg("name", new StringArg())
          .exec((sender, ctx) -> { /* apply kit */ return true; });

        sub("give") // `/kit give <player> <name> [silent]`
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
