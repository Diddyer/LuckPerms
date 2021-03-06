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

package me.lucko.luckperms.common.api;

import lombok.AllArgsConstructor;
import lombok.NonNull;

import com.google.common.eventbus.EventBus;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.Datastore;
import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.LPConfiguration;
import me.lucko.luckperms.api.Logger;
import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.api.MessagingService;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.PlatformType;
import me.lucko.luckperms.api.Storage;
import me.lucko.luckperms.api.Track;
import me.lucko.luckperms.api.User;
import me.lucko.luckperms.api.UuidCache;
import me.lucko.luckperms.api.context.ContextListener;
import me.lucko.luckperms.api.context.IContextCalculator;
import me.lucko.luckperms.api.event.LPEvent;
import me.lucko.luckperms.api.event.LPListener;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.api.internal.DatastoreLink;
import me.lucko.luckperms.common.api.internal.GroupLink;
import me.lucko.luckperms.common.api.internal.LPConfigurationLink;
import me.lucko.luckperms.common.api.internal.StorageLink;
import me.lucko.luckperms.common.api.internal.TrackLink;
import me.lucko.luckperms.common.api.internal.UserLink;
import me.lucko.luckperms.common.api.internal.Utils;
import me.lucko.luckperms.common.api.internal.UuidCacheLink;
import me.lucko.luckperms.common.core.NodeBuilder;
import me.lucko.luckperms.common.core.UserIdentifier;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static me.lucko.luckperms.common.api.internal.Utils.checkNode;

/**
 * Implements the LuckPerms API using the plugin instance
 */
@AllArgsConstructor
public class ApiProvider implements LuckPermsApi {
    private final LuckPermsPlugin plugin;
    private final EventBus eventBus = new EventBus("LuckPerms");

    public void fireEventAsync(LPEvent event) {
        plugin.doAsync(() -> fireEvent(event));
    }

    public void fireEvent(LPEvent event) {
        try {
            event.setApi(this);
            eventBus.post(event);
        } catch (Exception e) {
            getLogger().severe("Couldn't fire LuckPerms Event: " + event.getEventName());
            e.printStackTrace();
        }
    }

    @Override
    public void runUpdateTask() {
        plugin.getUpdateTaskBuffer().request();
    }

    @Override
    public double getApiVersion() {
        return 2.16;
    }

    @Override
    public String getVersion() {
        return plugin.getVersion();
    }

    @Override
    public PlatformType getPlatformType() {
        return plugin.getType();
    }

    @Override
    public void registerListener(@NonNull LPListener listener) {
        eventBus.register(listener);
    }

    @Override
    public void unregisterListener(@NonNull LPListener listener) {
        eventBus.unregister(listener);
    }

    @Override
    public LPConfiguration getConfiguration() {
        return new LPConfigurationLink(plugin.getConfiguration());
    }

    @Override
    public Storage getStorage() {
        return new StorageLink(plugin, plugin.getStorage());
    }

    @SuppressWarnings("deprecation")
    @Override
    public Datastore getDatastore() {
        return new DatastoreLink(plugin, plugin.getStorage());
    }

    @Override
    public Optional<MessagingService> getMessagingService() {
        return Optional.ofNullable(plugin.getRedisMessaging());
    }

    @Override
    public UuidCache getUuidCache() {
        return new UuidCacheLink(plugin.getUuidCache());
    }

    @Override
    public Logger getLogger() {
        return plugin.getLog();
    }

    @Override
    public User getUser(@NonNull UUID uuid) {
        final me.lucko.luckperms.common.core.model.User user = plugin.getUserManager().get(uuid);
        return user == null ? null : new UserLink(user);
    }

    @Override
    public Optional<User> getUserSafe(@NonNull UUID uuid) {
        return Optional.ofNullable(getUser(uuid));
    }

    @Override
    public User getUser(@NonNull String name) {
        final me.lucko.luckperms.common.core.model.User user = plugin.getUserManager().getByUsername(name);
        return user == null ? null : new UserLink(user);
    }

    @Override
    public Optional<User> getUserSafe(@NonNull String name) {
        return Optional.ofNullable(getUser(name));
    }

    @Override
    public Set<User> getUsers() {
        return plugin.getUserManager().getAll().values().stream().map(UserLink::new).collect(Collectors.toSet());
    }

    @Override
    public boolean isUserLoaded(@NonNull UUID uuid) {
        return plugin.getUserManager().isLoaded(UserIdentifier.of(uuid, null));
    }

    @Override
    public void cleanupUser(@NonNull User user) {
        Utils.checkUser(user);
        plugin.getUserManager().cleanup(((UserLink) user).getMaster());
    }

    @Override
    public Group getGroup(@NonNull String name) {
        final me.lucko.luckperms.common.core.model.Group group = plugin.getGroupManager().getIfLoaded(name);
        return group == null ? null : new GroupLink(group);
    }

    @Override
    public Optional<Group> getGroupSafe(@NonNull String name) {
        return Optional.ofNullable(getGroup(name));
    }

    @Override
    public Set<Group> getGroups() {
        return plugin.getGroupManager().getAll().values().stream().map(GroupLink::new).collect(Collectors.toSet());
    }

    @Override
    public boolean isGroupLoaded(@NonNull String name) {
        return plugin.getGroupManager().isLoaded(name);
    }

    @Override
    public Track getTrack(@NonNull String name) {
        final me.lucko.luckperms.common.core.model.Track track = plugin.getTrackManager().getIfLoaded(name);
        return track == null ? null : new TrackLink(track);
    }

    @Override
    public Optional<Track> getTrackSafe(@NonNull String name) {
        return Optional.ofNullable(getTrack(name));
    }

    @Override
    public Set<Track> getTracks() {
        return plugin.getTrackManager().getAll().values().stream().map(TrackLink::new).collect(Collectors.toSet());
    }

    @Override
    public boolean isTrackLoaded(@NonNull String name) {
        return plugin.getTrackManager().isLoaded(name);
    }

    @Override
    public Node.Builder buildNode(@NonNull String permission) throws IllegalArgumentException {
        return new NodeBuilder(checkNode(permission));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void registerContextCalculator(IContextCalculator<?> contextCalculator) {
        plugin.getContextManager().registerCalculator(contextCalculator);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void registerContextListener(ContextListener<?> contextListener) {
        plugin.getContextManager().registerListener(contextListener);
    }

    @Override
    public Optional<Contexts> getContextForUser(User user) {
        Utils.checkUser(user);
        return Optional.ofNullable(plugin.getContextForUser(((UserLink) user).getMaster()));
    }
}
