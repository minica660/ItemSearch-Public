package MiniCash.commands;

import MiniCash.ItemSearch;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

public class TeleportCommand implements BasicCommand {

    private final ItemSearch plugin;

    public TeleportCommand(ItemSearch plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSourceStack commandSourceStack, String[] args) {

        if(commandSourceStack.getExecutor() instanceof Player player) {

            if (args.length < 5) {
                player.sendMessage(ItemSearch.getMessage(Component.text("使用方法: /itemsearchtp <server> <world> <x> <y> <z>", NamedTextColor.RED)));
                return;
            }

            String targetServer = args[0];
            String targetWorldName = args[1];
            String currentServer = MiniCash.ItemSearch.getServerName();

            if (!currentServer.equalsIgnoreCase(targetServer)) {
                player.sendMessage(
                        ItemSearch.getMessage(
                                Component.text("指定したテレポート地点は別サーバーの" + targetServer +"にあるため、テレポートできません", NamedTextColor.RED)
                        )
                );
                return;
            }


            World targetWorld = plugin.getServer().getWorld(targetWorldName);

            if (targetWorld == null) {
                player.sendMessage(
                        ItemSearch.getMessage(Component.text("指定したワールド:" + targetWorldName + "は見つかりませんでした", NamedTextColor.RED))
                );
                return;
            }

            try {

                double x = Double.parseDouble(args[2]) + 0.5;
                double y = Double.parseDouble(args[3]);
                double z = Double.parseDouble(args[4]) + 0.5;

                Location targetLocation = new Location(targetWorld, x, y, z, player.getLocation().getYaw(), player.getLocation().getPitch());

                player.teleport(targetLocation);
                player.sendMessage(
                        ItemSearch.getMessage(
                                Component.text("テレポートしました! (" + targetWorldName + ": " + (int) x + ", " + (int) y + ", " + (int) z + ")", NamedTextColor.GREEN)
                        )
                );

            } catch (NumberFormatException e) {

                player.sendMessage(
                        ItemSearch.getMessage(
                                Component.text("座標には数値を入力してください", NamedTextColor.RED)
                        )
                );

            }


        }else {

            commandSourceStack.getSender().sendMessage(
                    ItemSearch.getMessage(
                            Component.text("このコマンドはプレイヤーのみ実行可能です",NamedTextColor.RED)
                    )
            );
        }

    }

    @Override
    public Collection<String> suggest(CommandSourceStack commandSourceStack, String[] args) {
        return BasicCommand.super.suggest(commandSourceStack, args);
    }

    @Override
    public @Nullable String permission() {
        return "itemsarch.command.teleport";
    }
}
