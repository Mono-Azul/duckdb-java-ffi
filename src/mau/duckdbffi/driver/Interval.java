package mau.duckdbffi.driver;

import java.time.Duration;
import java.time.Period;

// An Interval has a Period for intervals >= day and a Duration for interval parts < day.
// The objects must not be null, but initialized with 0
public record Interval(Period Period, Duration Duration)
{
}
