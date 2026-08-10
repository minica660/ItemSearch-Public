package MiniCash.commands;

import MiniCash.Database.DatabaseManager;
import MiniCash.model.SearchResult;
import MiniCash.util.ItemSerializer;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.jspecify.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;

public class ItemSearch implements BasicCommand {

    private final MiniCash.ItemSearch plugin;
    public ItemSearch(MiniCash.ItemSearch plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSourceStack commandSourceStack, String[] args) {

        if(!(commandSourceStack.getExecutor() instanceof Player)){
            commandSourceStack.getSender().sendMessage(
                    Component.text("このコマンドはプレイヤーのみ実行可能です").color(NamedTextColor.RED)
            );

            return;
        }

        Player player = (Player) commandSourceStack.getSender();

        if (args.length == 0) {
            sendHelp(player);
            return;
        }

        String sub = args[0].toLowerCase();
        Material material = null;
        Integer cmd = null;
        String name = null;
        String user = null;
        String world = null;
        Integer x = null, z = null, radius = null;
        String itemHash = null;

        switch (sub) {
            case "hand" -> {
                ItemStack handItem = player.getInventory().getItemInMainHand();
                if (handItem == null || handItem.getType().isAir()) {
                    player.sendMessage(Component.text("メインハンドにアイテムを持っていません", NamedTextColor.RED));
                    return;
                }

                ItemStack item = handItem.clone();
                item.setAmount(1);

                String base64 = ItemSerializer.itemSerializer(item);
                itemHash = ItemSerializer.getMD5Hash(base64);

            }
            case "mat" -> {
                if (args.length < 2) {
                    player.sendMessage(Component.text("使用方法: /itemsearch mat <Material> [CMD]", NamedTextColor.RED));
                    return;
                }
                material = Material.matchMaterial(args[1]);
                if (material == null) {
                    player.sendMessage(Component.text("指定された Material が存在しません: " + args[1], NamedTextColor.RED));
                    return;
                }
                if (args.length >= 3) {
                    try { cmd = Integer.parseInt(args[2]); } catch (NumberFormatException ignored) {}
                }
            }
            case "name" -> {
                if (args.length < 2) {
                    player.sendMessage(Component.text("使用方法: /itemsearch name <アイテム表示名>", NamedTextColor.RED));
                    return;
                }
                name = args[1];
            }
            case "user" -> {
                if (args.length < 2) {
                    player.sendMessage(Component.text("使用方法: /itemsearch user <プレイヤー名/UUID>", NamedTextColor.RED));
                    return;
                }
                user = args[1];
            }
            case "near" -> {
                radius = 30;
                if (args.length >= 2) {
                    try { radius = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) {}
                }
                world = player.getWorld().getName();
                x = player.getLocation().getBlockX();
                z = player.getLocation().getBlockZ();
            }
            default -> {
                sendHelp(player);
                return ;
            }
        }

        player.sendMessage(Component.text("🔍 データベースを検索中...", NamedTextColor.DARK_AQUA));

        DatabaseManager.searchItems(material, cmd, name, user, world, x, z, radius , itemHash).thenAccept(results -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> renderCoreProtectStyle(player, results));
        });

        return;


    }

    private void renderCoreProtectStyle(Player player, List<SearchResult> results) {

        if (results.isEmpty()) {
            player.sendMessage(Component.text("該当するアイテムは見つかりませんでした", NamedTextColor.RED));
            return;
        }

        player.sendMessage(Component.text("----- Item Search Result (" + results.size() + "件) -----", NamedTextColor.DARK_AQUA, TextDecoration.BOLD));

        for (SearchResult res : results) {

            String timeStr = (res.lastDate() != null)
                    ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(res.lastDate())
                    : "--:--";

            Component line1 = Component.text(timeStr + " ", NamedTextColor.GRAY)
                    .append(Component.text(res.containerType() + " ", NamedTextColor.DARK_AQUA));

            if (res.world() != null && res.x() != null) {
                line1 = line1.append(Component.text("@ " + res.world() + " " + res.x() + "," + res.y() + "," + res.z(), NamedTextColor.WHITE));
            } else {
                line1 = line1.append(Component.text("@ " + res.server(), NamedTextColor.WHITE));
            }
            player.sendMessage(line1);

            Component line2 = Component.text("  └ ", NamedTextColor.GRAY)
                    .append(Component.text(res.material().name(), NamedTextColor.GREEN))
                    .append(Component.text(" x" + res.amount(), NamedTextColor.GOLD));

            if (res.displayName() != null) {
                line2 = line2.append(Component.text(" (\"" + res.displayName() + "\")", NamedTextColor.AQUA));
            }

            if (res.customModelData() != null) {
                line2 = line2.append(Component.text(" [CMD:" + res.customModelData() + "]", NamedTextColor.LIGHT_PURPLE));
            }

            if (res.isNested() == 1) {
                line2 = line2.append(Component.text(" (内包)", NamedTextColor.DARK_GRAY));
            }

            if (res.finalEditorName() != null) {
                line2 = line2.append(Component.text(" [" + res.finalEditorName() + "]", NamedTextColor.YELLOW));
            }

            if (res.world() != null && res.x() != null) {
                String tpCmd = "/tp " + res.x() + " " + res.y() + " " + res.z();
                Component tpBtn = Component.text(" [TP]", NamedTextColor.DARK_GREEN, TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand(tpCmd))
                        .hoverEvent(HoverEvent.showText(Component.text("クリックで現地へテレポート\n" + tpCmd, NamedTextColor.GREEN)));
                line2 = line2.append(tpBtn);
            }

            Component infoHover = Component.text("Container ID: ", NamedTextColor.GRAY)
                    .append(Component.text(res.containerId(), NamedTextColor.WHITE))
                    .append(Component.text("\nEditor UUID: " + res.finalEditorUuid(), NamedTextColor.GRAY));

            Component infoBtn = Component.text(" [INFO]", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD)
                    .hoverEvent(HoverEvent.showText(infoHover));

            line2 = line2.append(infoBtn);

            player.sendMessage(line2);
        }

        player.sendMessage(Component.text("--------------------------------------------------", NamedTextColor.DARK_AQUA));
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.text("=== MiniCash ItemSearch ヘルプ ===", NamedTextColor.DARK_AQUA));
        player.sendMessage(Component.text("/itemsearch hand ", NamedTextColor.AQUA).append(Component.text("- メインハンドのアイテムで検索", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/itemsearch mat <Material> [CMD] ", NamedTextColor.AQUA).append(Component.text("- Material名で検索", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/itemsearch name <表示名> ", NamedTextColor.AQUA).append(Component.text("- 表示名で検索", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/itemsearch user <名/UUID> ", NamedTextColor.AQUA).append(Component.text("- 操作プレイヤーで検索", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/itemsearch near [半径] ", NamedTextColor.AQUA).append(Component.text("- 近くのコンテナから検索", NamedTextColor.GRAY)));
    }

    @Override
    public Collection<String> suggest(CommandSourceStack commandSourceStack, String[] args) {
        if (args.length == 1) {
            return List.of("hand", "mat", "name", "user", "near");
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if ("mat".equals(sub)) {
                String input = args[1].toLowerCase();
                return java.util.Arrays.stream(Material.values())
                        .filter(m -> !m.isAir())
                        .map(m -> m.name().toLowerCase())
                        .filter(name -> name.startsWith(input))
                        .limit(20) // 候補過多によるラグ防止
                        .toList();
            } else if ("user".equals(sub)) {
                return org.bukkit.Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .toList();
            } else if ("near".equals(sub)) {
                return List.of("10", "30", "50", "100");
            }
        }

        return List.of();
    }

    @Override
    public @Nullable String permission() {
        return "itemsearch.command.itemsearch";
    }
}
