package com.cnr_isac.oldmusa

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.cnr_isac.oldmusa.api.Channel
import com.cnr_isac.oldmusa.api.ChannelReading
import com.cnr_isac.oldmusa.api.Sensor
import com.cnr_isac.oldmusa.util.ApiUtil.api
import com.cnr_isac.oldmusa.util.ApiUtil.isAdmin
import com.cnr_isac.oldmusa.util.ApiUtil.query
import com.cnr_isac.oldmusa.util.ApiUtil.useLoadingBar
import com.cnr_isac.oldmusa.util.TimeUtil.midnightOf
import com.cnr_isac.oldmusa.util.TimeUtil.copy
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.android.synthetic.main.edit_channel.*
import kotlinx.android.synthetic.main.remove_channel.*
import java.text.SimpleDateFormat
import java.util.*


class QuickGraph : Fragment() {

    val args: QuickGraphArgs by navArgs()

    lateinit var chart: LineChart
    lateinit var data: LineData
    private lateinit var currentDate: Calendar

    private lateinit var currentChannel: Channel


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        setHasOptionsMenu(true)
        //getActivity()?.setTitle(currentChannel.name ?: "")
        getActivity()?.setTitle("Canale")

        val view = inflater.inflate(R.layout.fragment_quickgraph, container, false)

        view.findViewById<Button>(R.id.change_date).setOnClickListener {
            val datePicker = DatePickerDialog(context!!, { _, year, month, dayOfMonth ->
                onDateChange(midnightOf(year, month, dayOfMonth))
            }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DAY_OF_MONTH))
            datePicker.show()
        }

        chart = view.findViewById<LineChart>(R.id.chart).apply {
            setBackgroundColor(Color.WHITE)
            description.isEnabled = true
            setTouchEnabled(true)
            //setOnChartValueSelectedListener(this)
            setDrawGridBackground(true)
        }

        val xAxis = chart.xAxis
        xAxis.enableGridDashedLine(10f, 10f, 0f)
        xAxis.valueFormatter = object : ValueFormatter() {

            private val formatter = SimpleDateFormat.getTimeInstance()

            override fun getFormattedValue(value: Float): String {
                // Value saved as milliseconds from the start of the day
                val millis = value.toLong()
                return formatter.format(Date(currentDate.timeInMillis + millis))
            }
        }

        val yAxis = chart.axisLeft
        // disable dual axis (only use LEFT axis)
        chart.axisRight.isEnabled = false

        // horizontal grid lines
        yAxis.enableGridDashedLine(10f, 10f, 0f)


        onSensorLoad()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onSensorLoad()
    }

    fun onSensorLoad() {
        // Setup first date
        currentDate = midnightOf(2014, Calendar.APRIL, 1)// TODO: replace with LocalDate.now(), this is for testing purposes
        onDateChange(currentDate)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        isAdmin {
            if (!it) return@isAdmin

            inflater.inflate(R.menu.overflow_menu, menu)
            super.onCreateOptionsMenu(menu, inflater)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.remove -> {
                val mBuilder = AlertDialog.Builder(context!!)
                val dialog = mBuilder.setView(LayoutInflater.from(context!!).inflate(R.layout.remove_channel, null)).create()
                val lp = WindowManager.LayoutParams()
                lp.copyFrom(dialog.window!!.attributes)
                lp.width = (resources.displayMetrics.widthPixels * 0.75).toInt()
                lp.height = (resources.displayMetrics.heightPixels * 0.30).toInt()
                dialog.show()
                dialog.window!!.attributes = lp

                dialog.ButtonYesCha.setOnClickListener {
                    query {
                        currentChannel.delete()
                    }.onResult {
                        dialog.dismiss()
                        //reloadSite()
                    }
                }
                dialog.ButtonNoCha.setOnClickListener {
                    dialog.dismiss()
                }
            }
            R.id.edit -> {
                val mBuilder = AlertDialog.Builder(context!!)
                mBuilder.setTitle("Modifica il canale")
                val dialog = mBuilder.setView(LayoutInflater.from(context!!).inflate(R.layout.edit_channel, null)).create()
                val lp = WindowManager.LayoutParams()
                lp.copyFrom(dialog.window!!.attributes)
                lp.title = "modifica il canale"
                lp.width = (resources.displayMetrics.widthPixels * 0.85).toInt()
                lp.height = (resources.displayMetrics.heightPixels * 0.75).toInt()
                dialog.show()
                dialog.window!!.attributes = lp

                val nameCha = dialog.findViewById<EditText>(R.id.nameChannel)
                val unitCha = dialog.findViewById<EditText>(R.id.unitàMisura)
                val idcnrCha = dialog.findViewById<EditText>(R.id.IdCnrChannel)
                val minCha = dialog.findViewById<EditText>(R.id.minRange)
                val maxCha = dialog.findViewById<EditText>(R.id.maxRange)

                //nameCha.setText(currentChannel.name ?: "")
                //idcnrCha.setText(currentChannel.idCnr ?: "")
                //unitCha.setText(currentChannel.measureUnit ?: "")
                //minCha.setText(currentChannel.rangeMin?.toInt() ?: "")
                //maxCha.setText(currentChannel.rangeMax?.toDouble())

                dialog.aggiornaC.setOnClickListener {
                    /*query {
                        currentChannel.name = nameCha.text.toString()
                        //currentChannel.idCnr = idcnrCha.text.toString()
                        currentChannel.commit()
                    }.onResult {
                        dialog.dismiss()
                        //reload(View)
                    }*/
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun onDateChange(day: Calendar) {
        val channelId = args.channelId
        val from = day
        val to = from.copy()
        to.add(Calendar.DAY_OF_MONTH, 1)


        query {
            currentChannel = api.getChannel(channelId)
            currentChannel.getReadings(from.time, to.time)
        }.onResult {
            Log.d(TAG, "Data: ${userFriendlyDateFormatter.format(day.time)}")
            onDataReceived(day, it)
        }.useLoadingBar(this)
    }

    fun getPrimaryColor(): Int {
        val typedValue = TypedValue()

        val typedArray = context!!.obtainStyledAttributes(typedValue.data, intArrayOf(R.attr.colorPrimary))
        val color = typedArray.getColor(0, Color.RED)

        typedArray.recycle()

        return color
    }

    fun onDataReceived(day: Calendar, data: List<ChannelReading>) {
        currentDate = day

        view!!.findViewById<TextView>(R.id.date_text).text = getString(R.string.current_date).format(userFriendlyDateFormatter.format(day.time))

        val datasets = createData(currentChannel, data, day, getPrimaryColor())

        chart.data = LineData(datasets)
        chart.invalidate()// Refresh
        Log.i(TAG, "Data reloaded: $datasets")
    }

    fun createData(channel: Channel, data: List<ChannelReading>, start: Calendar, color: Int): LineDataSet {

        val vals = data.map {
            // Compress coordinates (from the 1970 to the beginning of the day
            // it should fit nicely in a float (with more precision, I hope)
            val diff = it.date.time - start.timeInMillis

            Entry(diff.toFloat(), it.valueMin.toFloat())
        }


        val dataSet = LineDataSet(vals, channel.name)
        dataSet.color = color
        dataSet.setCircleColor(color)
        return dataSet
    }

    companion object {
        private const val TAG = "QuickGraph"
        private val userFriendlyDateFormatter = SimpleDateFormat.getDateInstance()
    }
}