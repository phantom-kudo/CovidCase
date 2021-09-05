package com.phantomlabs.covidcase

enum class Metric {
    POSITIVE,NEGATIVE,DEATH
}
enum class TimeScale(val numDays: Int) {
    WEEK(7),
    MONTH(30),
    MAX(-1)
}