package com.sk89q.worldedit.command;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.extent.ResettableExtent;
import com.boydti.fawe.util.*;
import com.google.common.collect.Sets;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.extension.input.DisallowedUsageException;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.util.command.parametric.Optional;
import com.sk89q.worldedit.util.command.parametric.ParameterException;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.item.ItemTypes;

import java.io.FileNotFoundException;
import java.util.HashSet;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * General WorldEdit commands.
 */
@Command(aliases = {}, desc = "Player toggles, settings and item info")
public class OptionsCommands {

    private final WorldEdit worldEdit;

    /**
     * Create a new instance.
     *
     * @param worldEdit reference to WorldEdit
     */
    public OptionsCommands(WorldEdit worldEdit) {
        checkNotNull(worldEdit);
        this.worldEdit = worldEdit;
    }

    @Command(
            aliases = {"/tips", "tips"},
            desc = "Toggle FAWE tips"
    )
    public void tips(Player player, LocalSession session) throws WorldEditException {
        FawePlayer<Object> fp = FawePlayer.wrap(player);
        if (fp.toggle("fawe.tips")) {
            BBC.WORLDEDIT_TOGGLE_TIPS_ON.send(player);
        } else {
            BBC.WORLDEDIT_TOGGLE_TIPS_OFF.send(player);
        }
    }

    @Command(
            aliases = {"/fast"},
            usage = "[on|off]",
            desc = "Toggles FAWE undo",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.fast")
    public void fast(Player player, LocalSession session, CommandContext args) throws WorldEditException {

        String newState = args.getString(0, null);
        if (session.hasFastMode()) {
            if ("on".equals(newState)) {
                BBC.FAST_ENABLED.send(player);
                return;
            }

            session.setFastMode(false);
            BBC.FAST_DISABLED.send(player);
        } else {
            if ("off".equals(newState)) {
                BBC.FAST_DISABLED.send(player);
                return;
            }

            session.setFastMode(true);
            BBC.FAST_ENABLED.send(player);
        }
    }

    @Command(
            aliases = {"/gtexture", "gtexture"},
            usage = "[mask|#clipboard|complexity] [randomization=true]",
            help = "The global destination mask applies to all edits you do and masks based on the destination blocks (i.e. the blocks in the world).",
            desc = "Set the global mask",
            min = 0,
            max = -1
    )
    @CommandPermissions("worldedit.global-texture")
    public void gtexture(FawePlayer player, LocalSession session, EditSession editSession, @Optional CommandContext context) throws WorldEditException, FileNotFoundException, ParameterException {
        if (context == null || context.argsLength() == 0) {
            session.setTextureUtil(null);
            BBC.TEXTURE_DISABLED.send(player);
        } else {
            String arg = context.getString(0);
            String argLower = arg.toLowerCase();

            TextureUtil util = Fawe.get().getTextureUtil();
            int randomIndex = 1;
            boolean checkRandomization = true;
            if (context.argsLength() >= 2 && MathMan.isInteger(context.getString(0)) && MathMan.isInteger(context.getString(1))) {
                // complexity
                int min = Integer.parseInt(context.getString(0));
                int max = Integer.parseInt(context.getString(1));
                if (min < 0 || max > 100) throw new ParameterException("Complexity must be in the range 0-100");
                if (min != 0 || max != 100) util = new CleanTextureUtil(util, min, max);

                randomIndex = 2;
            } else if (context.argsLength() == 1 && argLower.equals("true") || argLower.equals("false")) {
                if (argLower.equals("true")) util = new RandomTextureUtil(util);
                checkRandomization = false;
            } else {
                HashSet<BaseBlock> blocks = null;
                if (argLower.equals("#copy") || argLower.equals("#clipboard")) {
                    Clipboard clipboard = player.getSession().getClipboard().getClipboard();
                    util = TextureUtil.fromClipboard(clipboard);
                } else if (argLower.equals("*") || argLower.equals("true")) {
                    util = Fawe.get().getTextureUtil();
                } else {
                    ParserContext parserContext = new ParserContext();
                    parserContext.setActor(player.getPlayer());
                    parserContext.setWorld(player.getWorld());
                    parserContext.setSession(session);
                    parserContext.setExtent(editSession);
                    Mask mask = worldEdit.getMaskFactory().parseFromInput(arg, parserContext);
                    util = TextureUtil.fromMask(mask);
                }
            }
            if (checkRandomization) {
                if (context.argsLength() > randomIndex) {
                    boolean random = Boolean.parseBoolean(context.getString(randomIndex));
                    if (random) util = new RandomTextureUtil(util);
                }
            }
            if (!(util instanceof CachedTextureUtil)) util = new CachedTextureUtil(util);
            session.setTextureUtil(util);
            BBC.TEXTURE_SET.send(player, context.getJoinedStrings(0));
        }
    }

    @Command(
            aliases = {"/gmask", "gmask", "globalmask", "/globalmask"},
            usage = "[mask]",
            help = "The global destination mask applies to all edits you do and masks based on the destination blocks (i.e. the blocks in the world).",
            desc = "Set the global mask",
            min = 0,
            max = -1
    )
    @CommandPermissions({"worldedit.global-mask", "worldedit.mask.global"})
    public void gmask(Player player, LocalSession session, EditSession editSession, @Optional CommandContext context) throws WorldEditException {
        if (context == null || context.argsLength() == 0) {
            session.setMask((Mask) null);
            BBC.MASK_DISABLED.send(player);
        } else {
            ParserContext parserContext = new ParserContext();
            parserContext.setActor(player);
            parserContext.setWorld(player.getWorld());
            parserContext.setSession(session);
            parserContext.setExtent(editSession);
            Mask mask = worldEdit.getMaskFactory().parseFromInput(context.getJoinedStrings(0), parserContext);
            session.setMask(mask);
            BBC.MASK.send(player);
        }
    }

    @Command(
            aliases = {"/gsmask", "gsmask", "globalsourcemask", "/globalsourcemask"},
            usage = "[mask]",
            desc = "Set the global source mask",
            help = "The global source mask applies to all edits you do and masks based on the source blocks (e.g. the blocks in your clipboard)",
            min = 0,
            max = -1
    )
    @CommandPermissions({"worldedit.global-mask", "worldedit.mask.global"})
    public void gsmask(Player player, LocalSession session, EditSession editSession, @Optional CommandContext context) throws WorldEditException {
        if (context == null || context.argsLength() == 0) {
            session.setSourceMask((Mask) null);
            BBC.SOURCE_MASK_DISABLED.send(player);
        } else {
            ParserContext parserContext = new ParserContext();
            parserContext.setActor(player);
            parserContext.setWorld(player.getWorld());
            parserContext.setSession(session);
            parserContext.setExtent(editSession);
            Mask mask = worldEdit.getMaskFactory().parseFromInput(context.getJoinedStrings(0), parserContext);
            session.setSourceMask(mask);
            BBC.SOURCE_MASK.send(player);
        }
    }

    @Command(
            aliases = {"/gtransform", "gtransform"},
            usage = "[transform]",
            desc = "Set the global transform",
            min = 0,
            max = -1
    )
    @CommandPermissions({"worldedit.global-transform", "worldedit.transform.global"})
    public void gtransform(Player player, EditSession editSession, LocalSession session, @Optional CommandContext context) throws WorldEditException {
        if (context == null || context.argsLength() == 0) {
            session.setTransform(null);
            BBC.TRANSFORM_DISABLED.send(player);
        } else {
            ParserContext parserContext = new ParserContext();
            parserContext.setActor(player);
            parserContext.setWorld(player.getWorld());
            parserContext.setSession(session);
            parserContext.setExtent(editSession);
            ResettableExtent transform = Fawe.get().getTransformParser().parseFromInput(context.getJoinedStrings(0), parserContext);
            session.setTransform(transform);
            BBC.TRANSFORM.send(player);
        }
    }

    @Command(
            aliases = {"/toggleplace", "toggleplace"},
            usage = "",
            desc = "Switch between your position and pos1 for placement",
            min = 0,
            max = 0
    )
    public void togglePlace(Player player, LocalSession session, CommandContext args) throws WorldEditException {

        if (session.togglePlacementPosition()) {
            BBC.PLACE_ENABLED.send(player);
        } else {
            BBC.PLACE_DISABLED.send(player);
        }
    }

    @Command(
            aliases = { "/timeout" },
            usage = "[time]",
            desc = "Modify evaluation timeout time.",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.timeout")
    public void timeout(Player player, LocalSession session, CommandContext args) throws WorldEditException {

        LocalConfiguration config = worldEdit.getConfiguration();
        boolean mayDisable = player.hasPermission("worldedit.timeout.unrestricted");

        int limit = args.argsLength() == 0 ? config.calculationTimeout : Math.max(-1, args.getInteger(0));
        if (!mayDisable && config.maxCalculationTimeout > -1) {
            if (limit > config.maxCalculationTimeout) {
                player.printError(BBC.getPrefix() + "Your maximum allowable timeout is " + config.maxCalculationTimeout + " ms.");
                return;
            }
        }

        session.setTimeout(limit);

        if (limit != config.calculationTimeout) {
            player.print(BBC.getPrefix() + "Timeout time set to " + limit + " ms. (Use //timeout to go back to the default.)");
        } else {
            player.print(BBC.getPrefix() + "Timeout time set to " + limit + " ms.");
        }
    }

    @Command(
            aliases = { "/drawsel" },
            usage = "[on|off]",
            desc = "Toggle drawing the current selection",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.drawsel")
    public void drawSelection(Player player, LocalSession session, CommandContext args) throws WorldEditException {

        if (!WorldEdit.getInstance().getConfiguration().serverSideCUI) {
            throw new DisallowedUsageException(BBC.getPrefix() + "This functionality is disabled in the configuration!");
        }
        String newState = args.getString(0, null);
        if (session.shouldUseServerCUI()) {
            if ("on".equals(newState)) {
                player.printError(BBC.getPrefix() + "Server CUI already enabled.");
                return;
            }

            session.setUseServerCUI(false);
            session.updateServerCUI(player);
            player.print(BBC.getPrefix() + "Server CUI disabled.");
        } else {
            if ("off".equals(newState)) {
                player.printError(BBC.getPrefix() + "Server CUI already disabled.");
                return;
            }

            session.setUseServerCUI(true);
            session.updateServerCUI(player);
            player.print(BBC.getPrefix() + "Server CUI enabled. This only supports cuboid regions, with a maximum size of 32x32x32.");
        }
    }

    @Command(
            aliases = {"/searchitem", "/l", "/search", "searchitem"},
            usage = "<query>",
            flags = "bi",
            desc = "Search for an item",
            help =
                    "Searches for an item.\n" +
                            "Flags:\n" +
                            "  -b only search for blocks\n" +
                            "  -i only search for items",
            min = 1,
            max = 1
    )
    public void searchItem(Actor actor, CommandContext args) throws WorldEditException {

        String query = args.getString(0).trim().toLowerCase();
        boolean blocksOnly = args.hasFlag('b');
        boolean itemsOnly = args.hasFlag('i');

        ItemType type = ItemTypes.get(query);

        if (type != null) {
            actor.print(BBC.getPrefix() + type.getId() + " (" + type.getName() + ")");
        } else {
            if (query.length() <= 2) {
                actor.printError(BBC.getPrefix() + "Enter a longer search string (len > 2).");
                return;
            }

            if (!blocksOnly && !itemsOnly) {
                actor.print(BBC.getPrefix() + "Searching for: " + query);
            } else if (blocksOnly && itemsOnly) {
                actor.printError(BBC.getPrefix() + "You cannot use both the 'b' and 'i' flags simultaneously.");
                return;
            } else if (blocksOnly) {
                actor.print(BBC.getPrefix() + "Searching for blocks: " + query);
            } else {
                actor.print(BBC.getPrefix() + "Searching for items: " + query);
            }

            int found = 0;

            for (ItemType searchType : ItemType.REGISTRY) {
                if (found >= 15) {
                    actor.print(BBC.getPrefix() + "Too many results!");
                    break;
                }

                if (blocksOnly && !searchType.hasBlockType()) {
                    continue;
                }

                if (itemsOnly && searchType.hasBlockType()) {
                    continue;
                }

                for (String alias : Sets.newHashSet(searchType.getId(), searchType.getName())) {
                    if (alias.contains(query)) {
                        actor.print(BBC.getPrefix() + searchType.getId() + " (" + searchType.getName() + ")");
                        ++found;
                        break;
                    }
                }
            }

            if (found == 0) {
                actor.printError(BBC.getPrefix() + "No items found.");
            }
        }
    }


}
