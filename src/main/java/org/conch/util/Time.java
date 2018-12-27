/*
 *  Copyright © 2017-2018 Sharder Foundation.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  version 2 as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, you can visit it at:
 *  https://www.gnu.org/licenses/old-licenses/gpl-2.0.txt
 *
 *  This software uses third party libraries and open-source programs,
 *  distributed under licenses described in 3RD-PARTY-LICENSES.
 *
 */

package org.conch.util;

import java.util.concurrent.atomic.AtomicInteger;

public interface Time {

    int getTime();

    final class EpochTime implements Time {

        public int getTime() {
            return Convert.toEpochTime(System.currentTimeMillis());
        }

    }

    final class ConstantTime implements Time {

        private final int time;

        public ConstantTime(int time) {
            this.time = time;
        }

        public int getTime() {
            return time;
        }

    }

    final class FasterTime implements Time {

        private final int multiplier;
        private final long systemStartTime;
        private final int time;

        public FasterTime(int time, int multiplier) {
            if (multiplier > 1000 || multiplier <= 0) {
                throw new IllegalArgumentException("Time multiplier must be between 1 and 1000");
            }
            this.multiplier = multiplier;
            this.time = time;
            this.systemStartTime = System.currentTimeMillis();
        }

        public int getTime() {
            return time + (int)((System.currentTimeMillis() - systemStartTime) / (1000 / multiplier));
        }

    }

    final class CounterTime implements Time {

        private final AtomicInteger counter;

        public CounterTime(int time) {
            this.counter = new AtomicInteger(time);
        }

        public int getTime() {
            return counter.incrementAndGet();
        }

    }

}
