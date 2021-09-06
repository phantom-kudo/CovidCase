package com.phantomlabs.covidcase

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.google.gson.GsonBuilder
import com.robinhood.spark.SparkAdapter
import com.robinhood.spark.SparkView
import com.robinhood.ticker.TickerUtils
import com.robinhood.ticker.TickerView
import org.angmarch.views.NiceSpinner
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.Exception
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*


private const val BASE_URL = "https://api.covidtracking.com/v1/"
private const val TAG = "MainActivity"
private const val ALL_STATES:String = "All (Nationwide)"
class MainActivity : AppCompatActivity() {
    private lateinit var currentShownData: List<CovidData>
    private lateinit var adapter: CovidSparkAdapter
    private lateinit var perStateDailyData: Map<String,List<CovidData>>
    private lateinit var nationalDailyData: List<CovidData>
    lateinit var tvMetricLabel:TickerView
    lateinit var tvDateLabel:TextView
    lateinit var radioBtnPositive:RadioButton
    lateinit var radioBtnNegative:RadioButton
    lateinit var radioBtnDeath:RadioButton
    lateinit var radioBtnWeek:RadioButton
    lateinit var radioBtnMonth:RadioButton
    lateinit var radioBtnMax:RadioButton
    lateinit var sparkView:SparkView
    lateinit var radioGroupPeriodSelection:RadioGroup
    lateinit var radioGroupMetricSelection:RadioGroup
    lateinit var selectedSpinner:NiceSpinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvMetricLabel = findViewById(R.id.tickerView)
        tvDateLabel = findViewById(R.id.tvDateLabel)
        radioBtnPositive = findViewById(R.id.radioButtonPositive)
        radioBtnNegative = findViewById(R.id.radioButtonNegative)
        radioBtnDeath= findViewById(R.id.radioButtonDeath)
        radioBtnMax = findViewById(R.id.rdobtnMax)
        radioBtnMonth = findViewById(R.id.rdobtnMonth)
        radioBtnWeek = findViewById(R.id.rdobtnWeek)
        sparkView = findViewById(R.id.sparkView)
        radioGroupPeriodSelection = findViewById(R.id.radioGroupPeriodSelection)
        radioGroupMetricSelection = findViewById(R.id.radioGroupMetricSelection)
        selectedSpinner = findViewById(R.id.spinnerSelect)

        supportActionBar?.title = getString(R.string.app_description)

        val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        val covidServices = retrofit.create(CovidServices::class.java)
        //Fetching data nation wide
        covidServices.getNationalData().enqueue(object: Callback<List<CovidData>>{
            override fun onResponse(call: Call<List<CovidData>>, response: Response<List<CovidData>>) {
                Log.i(TAG,"On Response $response")
                val nationalData = response.body()
                if(nationalData == null) {
                    Log.w(TAG, "Did not recieved a valid Data")
                    return
                }
                setUpEventListener()
                nationalDailyData = nationalData.reversed()
                Log.i(TAG,"Update the graph")
                updateDisplayWithData(nationalDailyData)
            }

            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG,"On Failure $t")
            }

        })
        //Fetching data state wise
        covidServices.getStateData().enqueue(object: Callback<List<CovidData>>{
            override fun onResponse(call: Call<List<CovidData>>, response: Response<List<CovidData>>) {
                Log.i(TAG,"On Response $response")
                val statesDate = response.body()
                if(statesDate == null) {
                    Log.w(TAG, "Did not recieved a valid Data")
                    return
                }
                perStateDailyData = statesDate.reversed().groupBy{ it.state }
                Log.i(TAG,"Update spinner with date")
                updateSpinnerWithStateData(perStateDailyData.keys)
            }

            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG,"On Failure $t")
            }

        })
    }

    private fun updateSpinnerWithStateData(stateName: Set<String>) {
        val stateAbbreviationList = stateName.toMutableList()
        stateAbbreviationList.sort()
        stateAbbreviationList.add(0,ALL_STATES)

        //Add state list as a data source for the spinner
        selectedSpinner.attachDataSource(stateAbbreviationList)
        selectedSpinner.setOnSpinnerItemSelectedListener { parent, _ , position, _ ->
            val selectedState = parent.getItemAtPosition(position) as String
            val selectedData = perStateDailyData[selectedState] ?: nationalDailyData
            updateDisplayWithData(selectedData)
        }

    }

    private fun setUpEventListener() {
        //Updating the TickerView
        tvMetricLabel.setCharacterLists(TickerUtils.provideNumberList())

        //Add a listener when user scrubs over the screen
        sparkView.isScrubEnabled = true
        sparkView.setScrubListener { itemData ->
            if(itemData is CovidData) {
                updateInfoForDate(itemData)
            }
        }
        //Respond to Radio Button Selected Event
        radioGroupPeriodSelection.setOnCheckedChangeListener { _, i ->
            adapter.daysAgo = when(i) {
                R.id.rdobtnWeek -> TimeScale.WEEK
                R.id.rdobtnMonth -> TimeScale.MONTH
                else -> TimeScale.MAX
            }
            adapter.notifyDataSetChanged()
        }

        radioGroupMetricSelection.setOnCheckedChangeListener { _, i ->
            when(i) {
                R.id.radioButtonPositive -> updateDisplayMetric(Metric.POSITIVE)
                R.id.radioButtonNegative -> updateDisplayMetric(Metric.NEGATIVE)
                else -> updateDisplayMetric(Metric.DEATH)
            }
        }
    }

    private fun updateDisplayMetric(metric: Metric) {
        val colorRes = when(metric) {
            Metric.POSITIVE -> R.color.colorPositive
            Metric.NEGATIVE -> R.color.colorNegative
            Metric.DEATH -> R.color.colorDeath
        }
        @ColorInt val colorInt = ContextCompat.getColor(this,colorRes)
        sparkView.lineColor = colorInt
        tvDateLabel.setTextColor(colorInt)

        //Update the metric on the adapter
        adapter.metric = metric
        adapter.notifyDataSetChanged()
        //Reset the Date and Data on the bottom textView
        updateInfoForDate(currentShownData.last())
    }

    fun updateDisplayWithData(dailyData: List<CovidData>) {
        currentShownData = dailyData
        //Create a new sparkAdapter with data
        adapter = CovidSparkAdapter(dailyData)
         sparkView.adapter = adapter
        //Update radio button to select the positive cases and max by default
        radioBtnPositive.isChecked = true
        radioBtnMax.isChecked = true
        //Display metric for most recent date
        updateDisplayMetric(Metric.POSITIVE)

    }

    fun updateInfoForDate(covidData: CovidData) {
        val numCases = when(adapter.metric) {
            Metric.POSITIVE -> covidData.positiveIncrease
            Metric.NEGATIVE -> covidData.negativeIncrease
            Metric.DEATH -> covidData.deathIncrease
        }
        tvMetricLabel.text = NumberFormat.getInstance().format(numCases)
        val outputDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        tvDateLabel.text = outputDateFormat.format(covidData.dateChecked)

    }

}