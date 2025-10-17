/*
 * duckdb-java-ffi - A DuckDB (non JDBC) Java client using FFI
 *
 * Copyright (C) 2025  Jens Hofer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package mau.duckdbffi.driver;

import java.time.Duration;
import java.time.Period;
import java.util.Comparator;

// An Interval has a Period for intervals >= day and a Duration for interval parts < day.
// The objects must not be null, but initialized with 0
public record Interval(Period Period, Duration Duration)
{
    public static final Comparator<Interval> COMPARATOR =
            // Compare Period's Years
            Comparator.comparing((Interval i) -> i.Period().getYears())
                    // Then compare Period's Months
                    .thenComparing((Interval i) -> i.Period().getMonths())
                    // Then compare Period's Days
                    .thenComparing((Interval i) -> i.Period().getDays())
                    // Finally, compare Duration
                    .thenComparing(Interval::Duration);

    public Interval normalized()
    {
        return new Interval(Period.normalized(), Duration);
    }
}
