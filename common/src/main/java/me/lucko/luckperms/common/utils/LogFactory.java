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

package me.lucko.luckperms.common.utils;

import lombok.experimental.UtilityClass;

import me.lucko.luckperms.api.Logger;

/**
 * Utility to help create wrapped log instances
 */
@UtilityClass
public class LogFactory {
    public static Logger wrap(org.slf4j.Logger l) {
        return new Logger() {
            private final org.slf4j.Logger logger = l;

            @Override
            public void info(String s) {
                logger.info(s);
            }

            @Override
            public void warn(String s) {
                logger.warn(s);
            }

            @Override
            public void severe(String s) {
                logger.error(s);
            }
        };
    }

    public static Logger wrap(java.util.logging.Logger l) {
        return new Logger() {
            private final java.util.logging.Logger logger = l;

            @Override
            public void info(String s) {
                logger.info(s);
            }

            @Override
            public void warn(String s) {
                logger.warning(s);
            }

            @Override
            public void severe(String s) {
                logger.severe(s);
            }
        };
    }
}
