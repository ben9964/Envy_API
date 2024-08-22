package com.envyful.api.forge.platform;

import com.envyful.api.concurrency.UtilLogger;
import com.envyful.api.forge.InitializationTask;
import com.envyful.api.forge.Initialized;
import com.envyful.api.forge.chat.UtilChatColour;
import com.envyful.api.forge.server.UtilForgeServer;
import com.envyful.api.platform.PlatformHandler;
import com.envyful.api.text.Placeholder;
import com.envyful.api.text.PlaceholderFactory;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.concurrent.TickDelayedTask;
import net.minecraft.util.text.ChatType;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraftforge.forgespi.language.ModFileScanData;
import net.minecraftforge.server.permission.PermissionAPI;
import org.objectweb.asm.Type;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

public class ForgePlatformHandler implements PlatformHandler<ICommandSource> {

    private static final Type DATA_ANNOTATION = Type.getType(Initialized.class);
    private static final ForgePlatformHandler INSTANCE = new ForgePlatformHandler();

    private ForgePlatformHandler() {
        ModList.get().getAllScanData().stream()
                .map(ModFileScanData::getAnnotations)
                .flatMap(Collection::stream)
                .filter(a -> DATA_ANNOTATION.equals(a.getAnnotationType()))
                .forEach(annotationData -> {
                    try {
                        var clazz = Class.forName(annotationData.getClassType().getClassName());
                        var constructor = clazz.getConstructor();
                        var initializationTask = (InitializationTask) constructor.newInstance();

                        initializationTask.run();
                    } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException |
                             IllegalAccessException | IllegalArgumentException |
                             InvocationTargetException e) {
                        UtilLogger.logger().ifPresent(logger -> logger.error("Error loading class", e));
                    }
                });
    }

    public static PlatformHandler<ICommandSource> getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean hasPermission(ICommandSource player, String permission) {
        if (player == null || permission == null) {
            return false;
        }

        return (isOP(player) || PermissionAPI.hasPermission((PlayerEntity) player, permission));
    }

    @Override
    public boolean isOP(ICommandSource sender) {
        if (!(sender instanceof PlayerEntity)) {
            return true;
        }

        var server = ServerLifecycleHooks.getCurrentServer();

        if (server == null) {
            return false;
        }

        var entry = server.getPlayerList().getOps().get(((PlayerEntity) sender).getGameProfile());
        return entry != null;
    }

    @Override
    public void broadcastMessage(Collection<String> message, Placeholder... placeholders) {
        for (var parsedMessage : UtilChatColour.colour(message, placeholders)) {
            ServerLifecycleHooks.getCurrentServer().getPlayerList().broadcastMessage(parsedMessage, ChatType.SYSTEM, Util.NIL_UUID);
        }
    }

    @Override
    public void sendMessage(ICommandSource player, Collection<String> message, Placeholder... placeholders) {
        for (var parsedMessage : UtilChatColour.colour(message, placeholders)) {
            player.sendMessage(parsedMessage, Util.NIL_UUID);
        }
    }

    @Override
    public void runSync(Runnable runnable) {
        ServerLifecycleHooks.getCurrentServer().execute(runnable);
    }

    @Override
    public void runLater(Runnable runnable, int delayTicks) {
        ServerLifecycleHooks.getCurrentServer().tell(new TickDelayedTask(
                        ServerLifecycleHooks.getCurrentServer().getTickCount() + delayTicks,
                runnable));
    }

    @Override
    public double getTPS() {
        return ServerLifecycleHooks.getCurrentServer().getAverageTickTime();
    }

    @Override
    public void executeConsoleCommands(List<String> commands, Placeholder... placeholders) {
        for (String command : commands) {
            for (String handlePlaceholder : PlaceholderFactory.handlePlaceholders(command, placeholders)) {
                UtilForgeServer.executeCommand(handlePlaceholder);
            }
        }
    }
}
