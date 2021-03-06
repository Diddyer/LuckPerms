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

package me.lucko.luckperms.api.data;

import java.util.function.Consumer;

/**
 * A callback used to wait for the completion of asynchronous operations.
 * All callbacks are ran on the main server thread.
 *
 * @param <T> the return type
 * @deprecated in favour of {@link Consumer}
 */
@Deprecated
public interface Callback<T> {

    static <T> Callback<T> empty() {
        return t -> {
        };
    }

    static <T> Callback<T> of(Runnable runnable) {
        if (runnable == null) {
            throw new NullPointerException("runnable");
        }
        return t -> runnable.run();
    }

    static <T> Callback<T> of(Consumer<T> consumer) {
        if (consumer == null) {
            throw new NullPointerException("consumer");
        }
        return consumer::accept;
    }

    /**
     * Helper method for converting old {@link Callback}s to use the new {@link me.lucko.luckperms.api.Storage}
     * interface.
     *
     * @param callback the callback to convert
     * @param <T>      the return type
     * @return a consumer instance
     * @since 2.14
     * @deprecated in favour of just using {@link Consumer}s.
     */
    @Deprecated
    static <T> Consumer<T> convertToConsumer(Callback<T> callback) {
        return callback::onComplete;
    }

    /**
     * Called when the operation completes.
     *
     * @param t the return value, may be null
     */
    void onComplete(T t);

}
