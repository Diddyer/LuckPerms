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

package me.lucko.luckperms.api.event.events;

import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.api.event.CancellableEvent;

/**
 * Called before a LogEntry is broadcasted to players only with the notify permission.
 * The log entry will still be recorded in the datastore, regardless of the cancellation state of this event.
 * Cancelling this event only stops the broadcast.
 */
public class LogNotifyEvent extends CancellableEvent {

    /**
     * The log entry to be broadcasted
     */
    private final LogEntry entry;

    public LogNotifyEvent(LogEntry entry) {
        super("Log Notify Event");
        this.entry = entry;
    }

    public LogEntry getEntry() {
        return entry;
    }

}
