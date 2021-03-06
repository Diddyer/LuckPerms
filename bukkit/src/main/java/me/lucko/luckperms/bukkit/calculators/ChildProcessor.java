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

package me.lucko.luckperms.bukkit.calculators;

import lombok.RequiredArgsConstructor;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.bukkit.model.ChildPermissionProvider;
import me.lucko.luckperms.common.calculators.PermissionProcessor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class ChildProcessor implements PermissionProcessor {
    private final ChildPermissionProvider provider;
    private Map<String, Boolean> childPermissions = new ConcurrentHashMap<>();

    @Override
    public Tristate hasPermission(String permission) {
        Boolean b = childPermissions.get(permission);
        return b == null ? Tristate.UNDEFINED : Tristate.fromBoolean(b);
    }

    @Override
    public void updateBacking(Map<String, Boolean> map) {
        childPermissions.clear();
        for (Map.Entry<String, Boolean> e : map.entrySet()) {
            Map<String, Boolean> children = provider.getPermissions().get(e);
            if (children != null) {
                childPermissions.putAll(children);
            }
        }
    }
}
