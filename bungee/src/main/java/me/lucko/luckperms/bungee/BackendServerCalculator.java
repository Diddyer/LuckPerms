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

import com.google.common.collect.Maps;

import me.lucko.luckperms.api.context.ContextCalculator;
import me.lucko.luckperms.api.context.MutableContextSet;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.Map;

public class BackendServerCalculator extends ContextCalculator<ProxiedPlayer> implements Listener {
    private static final String WORLD_KEY = "world";

    private static String getServer(ProxiedPlayer player) {
        return player.getServer() == null ? null : (player.getServer().getInfo() == null ? null : player.getServer().getInfo().getName());
    }

    @Override
    public MutableContextSet giveApplicableContext(ProxiedPlayer subject, MutableContextSet accumulator) {
        String server = getServer(subject);

        if (server != null) {
            accumulator.add(Maps.immutableEntry(WORLD_KEY, server));
        }

        return accumulator;
    }

    @Override
    public boolean isContextApplicable(ProxiedPlayer subject, Map.Entry<String, String> context) {
        if (!context.getKey().equals(WORLD_KEY)) {
            return false;
        }

        String server = getServer(subject);
        return server != null && server.equals(context.getValue());
    }

    @EventHandler
    public void onPlayerServerSwitch(ServerSwitchEvent e) {
        pushUpdate(e.getPlayer(), Maps.immutableEntry("null", "null"), Maps.immutableEntry(WORLD_KEY, getServer(e.getPlayer())));
    }
}
