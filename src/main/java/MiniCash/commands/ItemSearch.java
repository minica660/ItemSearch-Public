package MiniCash.commands;

import MiniCash.Database.DatabaseManager;
import MiniCash.model.SearchResult;
import MiniCash.model.TopResult;
import MiniCash.util.ItemSerializer;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;

import static java.util.Arrays.stream;

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
            sendHelpMessage(player);
            return;
        }

        int page = 1;
        List<String> argList = new java.util.ArrayList<>(List.of(args));

        for (int i = 0; i < argList.size(); i++) {
            if ("--page".equalsIgnoreCase(argList.get(i)) && i + 1 < argList.size()) {
                try {
                    page = Integer.parseInt(argList.get(i + 1));
                    argList.remove(i + 1);
                    argList.remove(i);
                    break;
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (argList.isEmpty()) {
            sendHelpMessage(player);
            return;
        }

        // ページネーション用ボタンに埋め込むための元コマンド文字列を再構築 (例: "/itemsearch material diamond")
        String fullCommand = "/itemsearch " + String.join(" ", argList);

        String sub = args[0].toLowerCase();
        Material material = null;
        Integer customModelData = null;
        String displayName = null;
        String user = null;
        String world = null;
        Integer x = null, z = null, radius = null;
        String itemHash = null;

        switch (sub) {
            case "hand" -> {

                ItemStack handItem = player.getInventory().getItemInMainHand();
                if (handItem == null || handItem.getType().isAir()) {
                    player.sendMessage(MiniCash.ItemSearch.getMessage(Component.text("メインハンドにアイテムを持っていません", NamedTextColor.RED)));
                    return;
                }

                ItemStack item = handItem.clone();
                item.setAmount(1);

                String base64 = ItemSerializer.itemSerializer(item);
                itemHash = ItemSerializer.getMD5Hash(base64);

                if(args[1].equals("user")){
                    user = args[2];
                }else if(args[1].equals("near")){
                    radius = 30;
                    if (args.length >= 3) {
                        try {
                            radius = Integer.valueOf(args[1]);
                        } catch (NumberFormatException e) {
                            player.sendMessage(
                                    MiniCash.ItemSearch.getMessage(Component.text(e.getMessage()).color(NamedTextColor.RED))
                            );
                            return;
                        }
                    }
                    world = player.getWorld().getName();
                    x = player.getLocation().getBlockX();
                    z = player.getLocation().getBlockZ();
                }


            }
            case "material" -> {
                if (args.length < 2) {
                    player.sendMessage(MiniCash.ItemSearch.getMessage(Component.text("使用方法: /itemsearch material <Material> [CMD]", NamedTextColor.RED)));
                    return;
                }
                material = Material.matchMaterial(args[1]);
                if (material == null) {
                    player.sendMessage(MiniCash.ItemSearch.getMessage(Component.text(args[1] + " というMaterialは存在しません!", NamedTextColor.RED)));
                    return;
                }
                if (args.length >= 4) {

                    if (args[2].equals("cmd")) {

                        try {
                            customModelData = Integer.parseInt(args[3]);
                        } catch (NumberFormatException e) {
                            player.sendMessage(
                                    MiniCash.ItemSearch.getMessage(Component.text(e.getMessage()).color(NamedTextColor.RED))
                            );
                            return;
                        }

                    }else if (args[2].equals("display")) {

                        displayName = args[3];

                    }else if (args[2].equals("user")) {
                        user = args[3];
                    }
                }
                if(args.length >= 6) {

                    if (args[4].equals("cmd")) {

                        try {
                            customModelData = Integer.parseInt(args[5]);
                        } catch (NumberFormatException e) {
                            player.sendMessage(
                                    MiniCash.ItemSearch.getMessage(Component.text(e.getMessage()).color(NamedTextColor.RED))
                            );
                            return;
                        }

                    }else if (args[4].equals("display")) {
                        displayName = args[5];
                    }else if (args[4].equals("user")) {
                        user = args[5];
                    }

                }else if(args.length >= 8){

                    if(args[6].equals("cmd")) {
                        try {
                            customModelData = Integer.parseInt(args[7]);
                        }catch (NumberFormatException e) {
                            player.sendMessage(
                                    MiniCash.ItemSearch.getMessage(Component.text(e.getMessage()).color(NamedTextColor.RED))
                            );
                            return;
                        }

                    }else if (args[6].equals("display")) {
                        displayName = args[7];
                    }else if (args[6].equals("user")) {
                        user = args[7];
                    }

                }

            }
            case "display" -> {
                if (args.length < 2) {
                    player.sendMessage(MiniCash.ItemSearch.getMessage(Component.text("使用方法: /itemsearch display <displayName>", NamedTextColor.RED)));
                    return;
                }
                displayName = args[1];
            }
            case "user" -> {
                if (args.length < 2) {
                    player.sendMessage(MiniCash.ItemSearch.getMessage(Component.text("使用方法: /itemsearch user <player>", NamedTextColor.RED)));
                    return;
                }
                user = args[1];
            }
            case "near" -> {
                radius = 30;
                if (args.length >= 2) {
                    try {
                        radius = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ignored) {

                    }
                }
                world = player.getWorld().getName();
                x = player.getLocation().getBlockX();
                z = player.getLocation().getBlockZ();
            }
            case "cmd" -> {

                if (args.length < 2) {
                    player.sendMessage(MiniCash.ItemSearch.getMessage(Component.text("使用方法: /itemsearch cmd <cmd>", NamedTextColor.RED)));
                    return;
                }

                try {
                    customModelData = Integer.parseInt(args[1]);
                }catch (NumberFormatException e) {
                    player.sendMessage(MiniCash.ItemSearch.getMessage(Component.text(e.getMessage()).color(NamedTextColor.RED)));
                    return;
                }

            }
            case "top" -> {

                if (args.length < 2) {
                    player.sendMessage(MiniCash.ItemSearch.getMessage(Component.text("使用方法: /itemsearch top <hand | material> [material] [CMD]", NamedTextColor.RED)));
                    return;
                }

                String topType = args[1].toLowerCase();

                if ("hand".equals(topType)) {

                    ItemStack handItem = player.getInventory().getItemInMainHand();

                    if (handItem.getType().isAir()) {
                        player.sendMessage(
                                MiniCash.ItemSearch.getMessage(
                                        Component.text("メインハンドにアイテムを持っていません", NamedTextColor.RED)
                                )
                        );
                        return;
                    }

                    ItemStack item = handItem.clone();
                    item.setAmount(1);

                    String base64 = ItemSerializer.itemSerializer(item);
                    String hash = ItemSerializer.getMD5Hash(base64);

                    player.sendMessage(
                            MiniCash.ItemSearch.getMessage(
                                    Component.text("手持ちアイテムの所持ランキングを取得中...", NamedTextColor.DARK_AQUA)
                            )
                    );

                    DatabaseManager.getTopHolders(hash, null, null, 10).thenAccept(topList -> {
                        plugin.getServer().getScheduler().runTask(plugin, () -> sendsearchTopResult(player, topList, handItem.getType().name() + " (完全一致)"));
                    });
                    return;

                } else if ("material".equals(topType)) {
                    if (args.length < 3) {
                        player.sendMessage(MiniCash.ItemSearch.getMessage(Component.text("使用方法: /itemsearch top material <Material> [CMD]", NamedTextColor.RED)));
                        return;
                    }

                    Material matchMaterial = Material.matchMaterial(args[2]);
                    if (matchMaterial == null) {
                        player.sendMessage(MiniCash.ItemSearch.getMessage(Component.text(args[2] + " というMaterialは存在しません!", NamedTextColor.RED)));
                        return;
                    }

                    Integer cmd = null;
                    if (args.length >= 4) {
                        try {
                            cmd = Integer.parseInt(args[3]);
                        } catch (NumberFormatException e) {
                            player.sendMessage(MiniCash.ItemSearch.getMessage(Component.text("数値を入力してください", NamedTextColor.RED)));
                            return;
                        }
                    }

                    player.sendMessage(
                            MiniCash.ItemSearch.getMessage(
                                    Component.text(matchMaterial.name() + " の所持ランキングを取得中...", NamedTextColor.DARK_AQUA)
                            )
                    );

                    Integer finalCmd = cmd;

                    DatabaseManager.getTopHolders(null, matchMaterial, cmd, 10).thenAccept(topList -> {
                        String titleName = matchMaterial.name() + (finalCmd != null ? " [CMD:" + finalCmd + "]" : "");
                        plugin.getServer().getScheduler().runTask(plugin, () -> sendsearchTopResult(player, topList, titleName));
                    });

                    return;

                } else {
                    player.sendMessage(MiniCash.ItemSearch.getMessage(Component.text("使用方法: /itemsearch top <hand | material>", NamedTextColor.RED)));
                    return;
                }

            }
            default -> {
                sendHelpMessage(player);
                return ;
            }
        }

        final int targetPage = page;
        player.sendMessage(MiniCash.ItemSearch.getMessage(Component.text("データベースを検索中...", NamedTextColor.DARK_AQUA)));

        DatabaseManager.searchItems(material, customModelData, displayName, user, world, x, z, radius , itemHash).thenAccept(results -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> sendSearchResult(player, results , targetPage , fullCommand));
        });

        return;


    }

    private void sendSearchResult(Player player, List<SearchResult> results , int page , String command) {

        if (results.isEmpty()) {
            player.sendMessage(MiniCash.ItemSearch.getMessage(Component.text("該当するアイテムは見つかりませんでした", NamedTextColor.RED)));
            return;
        }


        int pageSize = 7;
        int totalResults = results.size();
        int maxPage = (int) Math.ceil((double) totalResults / pageSize);

        if (page < 1) page = 1;
        if (page > maxPage) page = maxPage;

        int startIndex = (page - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalResults);

        player.sendMessage(Component.text("===== Item Search Result (" + totalResults + "件 : " + page + "/" + maxPage + "ページ ) =====", NamedTextColor.DARK_AQUA, TextDecoration.BOLD));

        for (int i = startIndex; i < endIndex; i++) {

            SearchResult res = results.get(i);

            String timeStr = (res.lastDate() != null)
                    ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(res.lastDate())
                    : "--:--";

            Component line1 = Component.text(timeStr + " ", NamedTextColor.GRAY)
                    .append(Component.text(res.containerType() + " ", NamedTextColor.DARK_AQUA));

            if (res.world() != null && res.x() != null && res.server() != null) {
                line1 = line1.append(Component.text("@ " + res.server() + ":" + res.world() + " " + res.x() + "," + res.y() + "," + res.z(), NamedTextColor.WHITE));
            } else {
                line1 = line1.append(Component.text("@ " + res.server(), NamedTextColor.WHITE));
            }
            player.sendMessage(line1);


            // ホバーイベントの際、アイテムの説明欄を表示させる
            Component matComponent = Component.text(res.material().name(), NamedTextColor.GREEN);


            if (res.itemBase64() != null && !res.itemBase64().isEmpty()) {
                ItemStack item = ItemSerializer.deitemSerializer(res.itemBase64());
                if (item != null) {
                    matComponent = matComponent.hoverEvent(item.asHoverEvent());
                }
            }




            Component line2 = Component.text("  └ ", NamedTextColor.GRAY)
                    .append(matComponent)
                    .append(Component.text(" x" + res.amount(), NamedTextColor.GOLD));

            if (res.displayName() != null) {
                Component nameComponent = Component.text(" (\"" + res.displayName() + "\")", NamedTextColor.AQUA);

                if (res.itemBase64() != null && !res.itemBase64().isEmpty()) {
                    ItemStack item = ItemSerializer.deitemSerializer(res.itemBase64());
                    if (item != null) {
                        nameComponent = nameComponent.hoverEvent(item.asHoverEvent());
                    }
                }
                line2 = line2.append(nameComponent);
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

                String teleportCommand = String.format("/itemsearchtp %s %s %d %d %d",
                        res.server(), res.world(), res.x(), res.y(), res.z());

                String hoverMessage = "クリックでテレポート\n" + teleportCommand;
                if (res.isNested() == 1) {
                    hoverMessage += "\n※このアイテムが入っているコンテナの位置へテレポート";
                }

                Component tpBtn = Component.text(" [TP]", NamedTextColor.DARK_GREEN, TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand(teleportCommand))
                        .hoverEvent(HoverEvent.showText(Component.text(hoverMessage, NamedTextColor.GREEN)));
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


        Component footer = Component.text("----------------", NamedTextColor.DARK_AQUA);


        // 前のページを表示
        if (page > 1) {
            String prevCmd = command + " --page " + (page - 1);
            Component prevBtn = Component.text(" [< 前へ]", NamedTextColor.YELLOW, TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand(prevCmd))
                    .hoverEvent(HoverEvent.showText(Component.text("前のページへ (" + (page - 1) + "ページ)", NamedTextColor.YELLOW)));
            footer = footer.append(prevBtn);
        } else {
            footer = footer.append(Component.text(" [< 前へ]", NamedTextColor.GRAY));
        }

        footer = footer.append(Component.text(" (" + page + "/" + maxPage + ") ", NamedTextColor.AQUA));

        // 次のページを表示
        if (page < maxPage) {
            String nextCmd = command + " --page " + (page + 1);
            Component nextBtn = Component.text("[次へ >] ", NamedTextColor.YELLOW, TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand(nextCmd))
                    .hoverEvent(HoverEvent.showText(Component.text("次のページへ (" + (page + 1) + "ページ)", NamedTextColor.YELLOW)));
            footer = footer.append(nextBtn);
        } else {
            footer = footer.append(Component.text("[次へ >] ", NamedTextColor.GRAY));
        }

        footer = footer.append(Component.text("----------------", NamedTextColor.DARK_AQUA));

        player.sendMessage(footer);
    }


    private void sendsearchTopResult(Player player, List<TopResult> topList, String targetName) {
        if (topList.isEmpty()) {
            player.sendMessage(MiniCash.ItemSearch.getMessage(Component.text("対象のアイテムを所持しているプレイヤーは見つかりませんでした", NamedTextColor.RED)));
            return;
        }

        player.sendMessage(Component.text("===== アイテム所持ランキング Top 10 [" + targetName + "] =====", NamedTextColor.GOLD, TextDecoration.BOLD));

        int rank = 1;
        for (TopResult res : topList) {
            NamedTextColor rankColor = switch (rank) {
                case 1 -> NamedTextColor.YELLOW;
                case 2 -> NamedTextColor.GRAY;
                case 3 -> NamedTextColor.GOLD;
                default -> NamedTextColor.WHITE;
            };

            Component line = Component.text("#" + rank + " ", rankColor, TextDecoration.BOLD)
                    .append(Component.text(res.playerName() + " ", NamedTextColor.AQUA))
                    .append(Component.text("- 合計: ", NamedTextColor.GRAY))
                    .append(Component.text(String.format("%,d個", res.totalAmount()), NamedTextColor.GREEN, TextDecoration.BOLD));

            Component infoHover = Component.text("UUID: " + res.playerUuid(), NamedTextColor.GRAY);
            Component infoBtn = Component.text(" [INFO]", NamedTextColor.DARK_PURPLE)
                    .hoverEvent(HoverEvent.showText(infoHover));

            line = line.append(infoBtn);

            player.sendMessage(line);
            rank++;
        }

        player.sendMessage(Component.text("--------------------------------------------------", NamedTextColor.GOLD));
    }


    private void sendHelpMessage(Player player) {
        player.sendMessage(Component.text("=== MiniCash ItemSearch ヘルプ ===", NamedTextColor.DARK_AQUA));
        player.sendMessage(Component.text("/itemsearch hand ", NamedTextColor.AQUA).append(Component.text("- メインハンドのアイテムで検索", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/itemsearch material <Material> [CMD] ", NamedTextColor.AQUA).append(Component.text("- Material名で検索", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/itemsearch display <displayName> ", NamedTextColor.AQUA).append(Component.text("- 表示名で検索", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/itemsearch user <player> ", NamedTextColor.AQUA).append(Component.text("- 操作プレイヤーで検索", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/itemsearch near [半径] ", NamedTextColor.AQUA).append(Component.text("- 近くのコンテナから検索", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/itemsearch cmd [value] ", NamedTextColor.AQUA).append(Component.text("- 値から検索", NamedTextColor.GRAY)));

    }

    @Override
    public Collection<String> suggest(CommandSourceStack commandSourceStack, String[] args) {
        if (args.length == 0 || args.length == 1) {
            return List.of("hand", "material", "display", "user", "near","cmd","top");
        }

        String sub = args[0].toLowerCase();

        if (args.length == 2) {


            if ("hand".equals(sub)) {
                return List.of("user", "near");

            } else if ("material".equals(sub)) {
                // Material表示
                String input = args[1].toLowerCase();
                return stream(Material.values())
                        .filter(m -> !m.isAir())
                        .map(m -> m.name().toLowerCase())
                        .filter(name -> name.startsWith(input))
                        .limit(20)
                        .toList();
            } else if ("user".equals(sub)) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .toList();
            } else if ("near".equals(sub)) {
                return List.of("10", "30", "50", "100");
            } else if ("cmd".equals(sub)) {
                return List.of("1","2","3","4","5","10");
            }else if ("top".equals(sub)) {
                return List.of("hand", "material");
            }

        }

        if ("hand".equals(sub)) {
            String handSub = args[1].toLowerCase();
            if ("user".equals(handSub) && args.length == 3) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                        .toList();
            } else if ("near".equals(handSub) && args.length == 3) {
                return List.of("10", "30", "50", "100");
            }
        }

        if ("material".equals(sub)) {
            if (args.length % 2 == 1) {

                return List.of("cmd", "display", "user");

            } else {
                String option = args[args.length - 2].toLowerCase();
                if ("user".equals(option)) {
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                            .toList();
                } else if ("cmd".equals(option)) {

                    return List.of("1", "2", "3", "4", "5", "10");

                }
            }
        }

        if (args.length == 3 && "top".equals(args[0]) && "material".equals(args[1])) {
            String input = args[2].toLowerCase();
            return stream(Material.values())
                    .filter(m -> !m.isAir())
                    .map(m -> m.name().toLowerCase())
                    .filter(name -> name.startsWith(input))
                    .limit(20)
                    .toList();
        }

        return List.of();
    }

    @Override
    public @Nullable String permission() {
        return "itemsearch.command.itemsearch";
    }
}
