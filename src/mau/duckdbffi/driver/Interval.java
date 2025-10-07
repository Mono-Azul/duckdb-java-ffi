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
}
