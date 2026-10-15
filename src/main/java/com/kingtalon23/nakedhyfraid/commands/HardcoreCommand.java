package com.kingtalon23.nakedhyfraid.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.kingtalon23.nakedhyfraid.NakedHyfraidPlugin;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * {@code /hardcore list} and {@code /hardcore revive <player>} for admins.
 */
public class HardcoreCommand extends CommandBase {
    public static final String PERMISSION = "nakedhyfraid.command.hardcore";

    public HardcoreCommand() {
        super("hardcore", "Naked & Hyfraid hardcore admin commands");
        this.requirePermission(PERMISSION);
        this.addSubCommand(new ListCommand());
        this.addSubCommand(new ReviveCommand());
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        context.sendMessage(Message.raw("/hardcore list - players who have died\n/hardcore revive <player> - let a dead player back in"));
    }

    static class ListCommand extends CommandBase {
        ListCommand() {
            super("list", "List players who died in hardcore");
            this.requirePermission(PERMISSION);
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            List<String> dead = NakedHyfraidPlugin.get().getDeadPlayers().list();
            context.sendMessage(Message.raw(dead.isEmpty() ? "Nobody has died yet." : "Dead: " + String.join(", ", dead)));
        }
    }

    static class ReviveCommand extends CommandBase {
        private final RequiredArg<String> player = this.withRequiredArg("player", "Username or UUID of the dead player", ArgTypes.STRING);

        ReviveCommand() {
            super("revive", "Let a dead player rejoin");
            this.requirePermission(PERMISSION);
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String target = this.player.get(context);
            String revived = NakedHyfraidPlugin.get().getDeadPlayers().revive(target);
            context.sendMessage(Message.raw(revived != null ? "Revived " + revived + ". They can rejoin now." : "No dead player called " + target + "."));
        }
    }
}
