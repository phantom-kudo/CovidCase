package com.phantomlabs.covidcase

import android.graphics.RectF
import com.robinhood.spark.SparkAdapter

class CovidSparkAdapter(private val dailyData: List<CovidData>) : SparkAdapter() {

    var metric = Metric.POSITIVE
    var daysAgo = TimeScale.MAX

    override fun getCount(): Int {
        return dailyData.size
    }

    override fun getItem(index: Int): Any {
        return dailyData[index]
    }

    override fun getY(index: Int): Float {
        val choosenDayData = dailyData[index]
        return when(metric) {
            Metric.NEGATIVE -> choosenDayData.negativeIncrease.toFloat()
            Metric.POSITIVE -> choosenDayData.positiveIncrease.toFloat()
            Metric.DEATH -> choosenDayData.deathIncrease.toFloat()
        }
    }

    override fun getDataBounds(): RectF {
        val bounds = super.getDataBounds()
        if(daysAgo != TimeScale.MAX) {
            bounds.left = count - daysAgo.numDays.toFloat()
        }
        return bounds
    }
}