/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.bungee;

import lombok.Getter;

import me.lucko.luckperms.ApiHandler;
import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.Logger;
import me.lucko.luckperms.api.PlatformType;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.MutableContextSet;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.api.ApiProvider;
import me.lucko.luckperms.common.caching.handlers.CachedStateManager;
import me.lucko.luckperms.common.calculators.CalculatorFactory;
import me.lucko.luckperms.common.commands.CommandManager;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.config.LPConfiguration;
import me.lucko.luckperms.common.contexts.ContextManager;
import me.lucko.luckperms.common.contexts.ServerCalculator;
import me.lucko.luckperms.common.core.UuidCache;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.data.Importer;
import me.lucko.luckperms.common.managers.GroupManager;
import me.lucko.luckperms.common.managers.TrackManager;
import me.lucko.luckperms.common.managers.UserManager;
import me.lucko.luckperms.common.managers.impl.GenericGroupManager;
import me.lucko.luckperms.common.managers.impl.GenericTrackManager;
import me.lucko.luckperms.common.managers.impl.GenericUserManager;
import me.lucko.luckperms.common.messaging.RedisMessaging;
import me.lucko.luckperms.common.storage.Storage;
import me.lucko.luckperms.common.storage.StorageFactory;
import me.lucko.luckperms.common.tasks.ExpireTemporaryTask;
import me.lucko.luckperms.common.tasks.UpdateTask;
import me.lucko.luckperms.common.utils.BufferedRequest;
import me.lucko.luckperms.common.utils.DebugHandler;
import me.lucko.luckperms.common.utils.LocaleManager;
import me.lucko.luckperms.common.utils.LogFactory;
import me.lucko.luckperms.common.utils.PermissionCache;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Getter
public class LPBungeePlugin extends Plugin implements LuckPermsPlugin {
    private final Set<UUID> ignoringLogs = ConcurrentHashMap.newKeySet();
    private Executor executor;
    private LPConfiguration configuration;
    private UserManager userManager;
    private GroupManager groupManager;
    private TrackManager trackManager;
    private Storage storage;
    private RedisMessaging redisMessaging = null;
    private UuidCache uuidCache;
    private ApiProvider apiProvider;
    private Logger log;
    private Importer importer;
    private LocaleManager localeManager;
    private CachedStateManager cachedStateManager;
    private ContextManager<ProxiedPlayer> contextManager;
    private CalculatorFactory calculatorFactory;
    private BufferedRequest<Void> updateTaskBuffer;
    private DebugHandler debugHandler;
    private BungeeSenderFactory senderFactory;
    private PermissionCache permissionCache;

    @Override
    public void onEnable() {
        executor = r -> getProxy().getScheduler().runAsync(this, r);
        log = LogFactory.wrap(getLogger());
        debugHandler = new DebugHandler(executor, getVersion());
        senderFactory = new BungeeSenderFactory(this);
        permissionCache = new PermissionCache(executor);

        getLog().info("Loading configuration...");
        configuration = new BungeeConfig(this);

        // register events
        getProxy().getPluginManager().registerListener(this, new BungeeListener(this));

        // initialise datastore
        storage = StorageFactory.getInstance(this, "h2");

        // initialise redis
        if (getConfiguration().isRedisEnabled()) {
            getLog().info("Loading redis...");
            redisMessaging = new RedisMessaging(this);
            try {
                redisMessaging.init(getConfiguration().getRedisAddress(), getConfiguration().getRedisPassword());
                getLog().info("Loaded redis successfully...");
            } catch (Exception e) {
                getLog().info("Couldn't load redis...");
                e.printStackTrace();
            }
        }

        // setup the update task buffer
        updateTaskBuffer = new BufferedRequest<Void>(1000L, this::doAsync) {
            @Override
            protected Void perform() {
                new UpdateTask(LPBungeePlugin.this).run();
                return null;
            }
        };

        // load locale
        localeManager = new LocaleManager();
        File locale = new File(getDataFolder(), "lang.yml");
        if (locale.exists()) {
            getLog().info("Found locale file. Attempting to load from it.");
            try {
                localeManager.loadFromFile(locale);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // register commands
        getLog().info("Registering commands...");
        CommandManager commandManager = new CommandManager(this);
        getProxy().getPluginManager().registerCommand(this, new BungeeCommand(this, commandManager));

        // disable the default Bungee /perms command so it gets handled by the Bukkit plugin
        getProxy().getDisabledCommands().add("perms");

        // load internal managers
        getLog().info("Loading internal permission managers...");
        uuidCache = new UuidCache(getConfiguration().isOnlineMode());
        userManager = new GenericUserManager(this);
        groupManager = new GenericGroupManager(this);
        trackManager = new GenericTrackManager();
        importer = new Importer(commandManager);
        calculatorFactory = new BungeeCalculatorFactory(this);
        cachedStateManager = new CachedStateManager(this);

        contextManager = new ContextManager<>();
        BackendServerCalculator serverCalculator = new BackendServerCalculator();
        getProxy().getPluginManager().registerListener(this, serverCalculator);
        contextManager.registerCalculator(serverCalculator);
        contextManager.registerCalculator(new ServerCalculator<>(getConfiguration().getServer()));

        // register with the LP API
        getLog().info("Registering API...");
        apiProvider = new ApiProvider(this);
        ApiHandler.registerProvider(apiProvider);

        // schedule update tasks
        int mins = getConfiguration().getSyncTime();
        if (mins > 0) {
            getProxy().getScheduler().schedule(this, new UpdateTask(this), mins, mins, TimeUnit.MINUTES);
        }

        // run an update instantly.
        updateTaskBuffer.requestDirectly();

        // register tasks
        getProxy().getScheduler().schedule(this, new ExpireTemporaryTask(this), 3L, 3L, TimeUnit.SECONDS);

        getLog().info("Successfully loaded.");
    }

    @Override
    public void onDisable() {
        getLog().info("Closing datastore...");
        storage.shutdown();

        if (redisMessaging != null) {
            getLog().info("Closing redis...");
            redisMessaging.shutdown();
        }

        getLog().info("Unregistering API...");
        ApiHandler.unregisterProvider();
    }

    @Override
    public String getVersion() {
        return getDescription().getVersion();
    }

    @Override
    public PlatformType getType() {
        return PlatformType.BUNGEE;
    }

    @Override
    public File getMainDir() {
        return getDataFolder();
    }

    @Override
    public ProxiedPlayer getPlayer(User user) {
        return getProxy().getPlayer(uuidCache.getExternalUUID(user.getUuid()));
    }

    @Override
    public Contexts getContextForUser(User user) {
        ProxiedPlayer player = getPlayer(user);
        if (player == null) {
            return null;
        }
        return new Contexts(
                getContextManager().getApplicableContext(player),
                getConfiguration().isIncludingGlobalPerms(),
                getConfiguration().isIncludingGlobalWorldPerms(),
                true,
                getConfiguration().isApplyingGlobalGroups(),
                getConfiguration().isApplyingGlobalWorldGroups(),
                false
        );
    }

    @Override
    public int getPlayerCount() {
        return getProxy().getOnlineCount();
    }

    @Override
    public List<String> getPlayerList() {
        return getProxy().getPlayers().stream().map(ProxiedPlayer::getName).collect(Collectors.toList());
    }

    @Override
    public Set<UUID> getOnlinePlayers() {
        return getProxy().getPlayers().stream().map(ProxiedPlayer::getUniqueId).collect(Collectors.toSet());
    }

    @Override
    public boolean isOnline(UUID external) {
        return getProxy().getPlayer(external) != null;
    }

    @Override
    public List<Sender> getSenders() {
        return getProxy().getPlayers().stream()
                .map(p -> getSenderFactory().wrap(p))
                .collect(Collectors.toList());
    }

    @Override
    public Sender getConsoleSender() {
        return getSenderFactory().wrap(getProxy().getConsole());
    }

    @Override
    public Set<Contexts> getPreProcessContexts(boolean op) {
        Set<ContextSet> c = new HashSet<>();
        c.add(ContextSet.empty());
        c.add(ContextSet.singleton("server", getConfiguration().getServer()));
        c.addAll(getProxy().getServers().values().stream()
                .map(ServerInfo::getName)
                .map(s -> {
                    MutableContextSet set = MutableContextSet.create();
                    set.add("server", getConfiguration().getServer());
                    set.add("world", s);
                    return set.makeImmutable();
                })
                .collect(Collectors.toList())
        );

        return c.stream()
                .map(set -> new Contexts(
                        set,
                        getConfiguration().isIncludingGlobalPerms(),
                        getConfiguration().isIncludingGlobalWorldPerms(),
                        true,
                        getConfiguration().isApplyingGlobalGroups(),
                        getConfiguration().isApplyingGlobalWorldGroups(),
                        false
                ))
                .collect(Collectors.toSet());
    }

    @Override
    public Object getPlugin(String name) {
        return getProxy().getPluginManager().getPlugin(name);
    }

    @Override
    public Object getService(Class clazz) {
        return null;
    }

    @Override
    public UUID getUUID(String playerName) {
        return null; // Not needed on Bungee
    }

    @Override
    public boolean isPluginLoaded(String name) {
        return getProxy().getPluginManager().getPlugins().stream()
                .filter(p -> p.getDescription().getName().equalsIgnoreCase(name))
                .findAny()
                .isPresent();
    }

    @Override
    public void doAsync(Runnable r) {
        getProxy().getScheduler().runAsync(this, r);
    }

    @Override
    public void doSync(Runnable r) {
        doAsync(r);
    }

    @Override
    public Executor getSyncExecutor() {
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return executor;
    }

    @Override
    public void doAsyncRepeating(Runnable r, long interval) {
        long millis = interval * 50L; // convert from ticks to milliseconds
        getProxy().getScheduler().schedule(this, r, millis, millis, TimeUnit.MILLISECONDS);
    }
}
