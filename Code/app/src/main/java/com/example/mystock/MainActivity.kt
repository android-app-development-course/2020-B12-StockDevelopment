package com.example.mystock

import android.R
import android.app.NotificationManager
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import java.util.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val sharedPref = getPreferences(MODE_PRIVATE)
        val idsStr = sharedPref.getString(StockIdsKey_, ShIndex + "," + SzIndex + "," + ChuangIndex)
        val ids = idsStr!!.split(",").toTypedArray()
        StockIds_.clear()
        for (id in ids) {
            StockIds_.add(id)
        }
        val timer = Timer("RefreshStocks")
        timer.schedule(object : TimerTask() {
            override fun run() {
                refreshStocks()
            }
        }, 0, 10000) // 10 seconds
    }

    public override fun onDestroy() {
        super.onDestroy() // Always call the superclass
        saveStocksToPreferences()
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        saveStocksToPreferences()

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        if (id == R.id.action_settings) {
            return true
        } else if (id == R.id.action_delete) {
            if (SelectedStockItems_.isEmpty()) return true
            for (selectedId in SelectedStockItems_) {
                StockIds_.remove(selectedId)
                val table = findViewById<View>(R.id.stock_table) as TableLayout
                val count = table.childCount
                for (i in 1 until count) {
                    val row = table.getChildAt(i) as TableRow
                    val nameId = row.getChildAt(0) as LinearLayout
                    val idText = nameId.getChildAt(1) as TextView
                    if (idText != null && idText.text.toString() === selectedId) {
                        table.removeView(row)
                        break
                    }
                }
            }
            SelectedStockItems_.clear()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun saveStocksToPreferences() {
        var ids: String? = ""
        for (id in StockIds_) {
            ids += id
            ids += ","
        }
        val sharedPref = getPreferences(MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putString(StockIdsKey_, ids)
        editor.commit()
    }

    // 浦发银行,15.06,15.16,15.25,15.27,14.96,15.22,15.24,205749026,3113080980,
    // 51800,15.22,55979,15.21,1404740,15.20,1016176,15.19,187800,15.18,300,15.24,457700,15.25,548900,15.26,712266,15.27,1057960,15.28,2015-09-10,15:04:07,00
    inner class Stock {
        var id_: String? = null
        var name_: String? = null
        var open_: String? = null
        var yesterday_: String? = null
        var now_: String? = null
        var high_: String? = null
        var low_: String? = null
        var b1_: String? = null
        var b2_: String? = null
        var b3_: String? = null
        var b4_: String? = null
        var b5_: String? = null
        var bp1_: String? = null
        var bp2_: String? = null
        var bp3_: String? = null
        var bp4_: String? = null
        var bp5_: String? = null
        var s1_: String? = null
        var s2_: String? = null
        var s3_: String? = null
        var s4_: String? = null
        var s5_: String? = null
        var sp1_: String? = null
        var sp2_: String? = null
        var sp3_: String? = null
        var sp4_: String? = null
        var sp5_: String? = null
        var time_: String? = null
    }

    fun sinaResponseToStocks(response: String): TreeMap<String?, Stock?> {
        var response = response
        response = response.replace("\n".toRegex(), "")
        val stocks = response.split(";").toTypedArray()
        val stockMap: TreeMap<String?, Stock?> = TreeMap<Any?, Any?>()
        for (stock in stocks) {
            val leftRight = stock.split("=").toTypedArray()
            if (leftRight.size < 2) continue
            val right = leftRight[1].replace("\"".toRegex(), "")
            if (right.isEmpty()) continue
            val left = leftRight[0]
            if (left.isEmpty()) continue
            val stockNow: Stock = Stock()
            stockNow.id_ = left.split("_").toTypedArray()[2]
            val values = right.split(",").toTypedArray()
            stockNow.name_ = values[0]
            stockNow.open_ = values[1]
            stockNow.yesterday_ = values[2]
            stockNow.now_ = values[3]
            stockNow.high_ = values[4]
            stockNow.low_ = values[5]
            stockNow.b1_ = values[10]
            stockNow.b2_ = values[12]
            stockNow.b3_ = values[14]
            stockNow.b4_ = values[16]
            stockNow.b5_ = values[18]
            stockNow.bp1_ = values[11]
            stockNow.bp2_ = values[13]
            stockNow.bp3_ = values[15]
            stockNow.bp4_ = values[17]
            stockNow.bp5_ = values[19]
            stockNow.s1_ = values[20]
            stockNow.s2_ = values[22]
            stockNow.s3_ = values[24]
            stockNow.s4_ = values[26]
            stockNow.s5_ = values[28]
            stockNow.sp1_ = values[21]
            stockNow.sp2_ = values[23]
            stockNow.sp3_ = values[25]
            stockNow.sp4_ = values[27]
            stockNow.sp5_ = values[29]
            stockNow.time_ = values[values.size - 3] + "_" + values[values.size - 2]
            stockMap[stockNow.id_] = stockNow
        }
        return stockMap
    }

    fun querySinaStocks(list: String?) {
        // Instantiate the RequestQueue.
        val queue: RequestQueue = Volley.newRequestQueue(this)
        val url = "http://hq.sinajs.cn/list=$list"
        //http://hq.sinajs.cn/list=sh600000,sh600536

        // Request a string response from the provided URL.
        val stringRequest = StringRequest(Request.Method.GET, url, object : Listener<String?>() {
                    fun onResponse(response: String) {
                        updateStockListView(sinaResponseToStocks(response))
                    }
                },
                object : ErrorListener() {
                    fun onErrorResponse(error: VolleyError?) {}
                })
        queue.add(stringRequest)
    }

    private fun refreshStocks() {
        var ids: String? = ""
        for (id in StockIds_) {
            ids += id
            ids += ","
        }
        querySinaStocks(ids)
    }

    fun addStock(view: View?) {
        val editText = findViewById<View>(R.id.editText_stockId) as EditText
        var stockId = editText.text.toString()
        if (stockId.length != 6) return
        stockId = if (stockId.startsWith("6")) {
            "sh$stockId"
        } else if (stockId.startsWith("0") || stockId.startsWith("3")) {
            "sz$stockId"
        } else return
        StockIds_.add(stockId)
        refreshStocks()
    }

    fun sendNotifation(id: Int, title: String?, text: String?) {
        val nBuilder = NotificationCompat.Builder(this)
        nBuilder.setSmallIcon(R.drawable.ic_launcher)
        nBuilder.setContentTitle(title)
        nBuilder.setContentText(text)
        nBuilder.setVibrate(longArrayOf(100, 100, 100))
        nBuilder.setLights(Color.RED, 1000, 1000)
        val notifyMgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notifyMgr.notify(id, nBuilder.build())
    }

    fun updateStockListView(stockMap: TreeMap<String?, Stock?>) {
//        String stock_name = getResources().getString(R.string.stock_name);
//        String stock_id = getResources().getString(R.string.stock_id);
//        String stock_now = getResources().getString(R.string.stock_now);

//        ListView list = (ListView) findViewById(R.id.listView);
//
//        ArrayList<HashMap<String, String>> stockList = new ArrayList<>();
//        HashMap<String, String> mapTitle = new HashMap<>();
//        mapTitle.put(stock_name, getResources().getString(R.string.stock_name_title));
//        //mapTitle.put(stock_id, "");
//        mapTitle.put(stock_now, getResources().getString(R.string.stock_now_title));
//        stockList.add(mapTitle);
//
//        for(Stock stock : stocks)
//        {
//            HashMap<String, String> map = new HashMap<>();
//            map.put(stock_name, stock.name_);
//            String id = stock.id_.replaceAll("sh", "");
//            id = id.replaceAll("sz", "");
//            map.put(stock_id, id);
//            map.put(stock_now, stock.now_);
//            stockList.add(map);
//        }
//
//        SimpleAdapter adapter = new SimpleAdapter(this,
//                stockList,
//                R.layout.stock_listitem,
//                new String[] {stock_name, stock_id, stock_now},
//                new int[] {R.id.stock_name, R.id.stock_id, R.id.stock_now});
//        list.setAdapter(adapter);

        // Table
        val table = findViewById<View>(R.id.stock_table) as TableLayout
        table.isStretchAllColumns = true
        table.isShrinkAllColumns = true
        table.removeAllViews()

        // Title
        val rowTitle = TableRow(this)
        val nameTitle = TextView(this)
        nameTitle.text = resources.getString(R.string.stock_name_title)
        rowTitle.addView(nameTitle)
        val nowTitle = TextView(this)
        nowTitle.gravity = Gravity.CENTER
        nowTitle.text = resources.getString(R.string.stock_now_title)
        rowTitle.addView(nowTitle)
        val percentTitle = TextView(this)
        percentTitle.gravity = Gravity.CENTER
        percentTitle.text = resources.getString(R.string.stock_increase_percent_title)
        rowTitle.addView(percentTitle)
        val increaseTitle = TextView(this)
        increaseTitle.gravity = Gravity.CENTER
        increaseTitle.text = resources.getString(R.string.stock_increase_title)
        rowTitle.addView(increaseTitle)
        table.addView(rowTitle)
        val stocks: Collection<Stock?> = stockMap.values
        for (stock in stocks) {
            if (stock!!.id_ == ShIndex || stock.id_ == SzIndex || stock.id_ == ChuangIndex) {
                val dNow = stock.now_!!.toDouble()
                val dYesterday = stock.yesterday_!!.toDouble()
                val dIncrease = dNow - dYesterday
                val dPercent = dIncrease / dYesterday * 100
                val change = String.format("%.2f", dPercent) + "% " + String.format("%.2f", dIncrease)
                var indexId: Int
                var changeId: Int
                if (stock.id_ == ShIndex) {
                    indexId = R.id.stock_sh_index
                    changeId = R.id.stock_sh_change
                } else if (stock.id_ == SzIndex) {
                    indexId = R.id.stock_sz_index
                    changeId = R.id.stock_sz_change
                } else {
                    indexId = R.id.stock_chuang_index
                    changeId = R.id.stock_chuang_change
                }
                val indexText = findViewById<View>(indexId) as TextView
                indexText.text = stock.now_
                var color = Color.BLACK
                if (dIncrease > 0) {
                    color = Color.RED
                } else if (dIncrease < 0) {
                    color = Color.GREEN
                }
                indexText.setTextColor(color)
                val changeText = findViewById<View>(changeId) as TextView
                changeText.text = change
                continue
            }
            val row = TableRow(this)
            row.gravity = Gravity.CENTER_VERTICAL
            if (SelectedStockItems_.contains(stock.id_)) {
                row.setBackgroundColor(HighlightColor_)
            }
            val nameId = LinearLayout(this)
            nameId.orientation = LinearLayout.VERTICAL
            val name = TextView(this)
            name.text = stock.name_
            nameId.addView(name)
            val id = TextView(this)
            id.textSize = 10f
            id.text = stock.id_
            nameId.addView(id)
            row.addView(nameId)
            val now = TextView(this)
            now.gravity = Gravity.RIGHT
            now.text = stock.now_
            row.addView(now)
            val percent = TextView(this)
            percent.gravity = Gravity.RIGHT
            val increaseValue = TextView(this)
            increaseValue.gravity = Gravity.RIGHT
            val dOpen = stock.open_!!.toDouble()
            val dB1 = stock.bp1_!!.toDouble()
            val dS1 = stock.sp1_!!.toDouble()
            if (dOpen == 0.0 && dB1 == 0.0 && dS1 == 0.0) {
                percent.text = "--"
                increaseValue.text = "--"
            } else {
                var dNow = stock.now_!!.toDouble()
                if (dNow == 0.0) { // before open
                    if (dS1 == 0.0) {
                        dNow = dB1
                        now.text = stock.bp1_
                    } else {
                        dNow = dS1
                        now.text = stock.sp1_
                    }
                }
                val dYesterday = stock.yesterday_!!.toDouble()
                val dIncrease = dNow - dYesterday
                val dPercent = dIncrease / dYesterday * 100
                percent.text = String.format("%.2f", dPercent) + "%"
                increaseValue.text = String.format("%.2f", dIncrease)
                var color = Color.BLACK
                if (dIncrease > 0) {
                    color = Color.RED
                } else if (dIncrease < 0) {
                    color = Color.GREEN
                }
                now.setTextColor(color)
                percent.setTextColor(color)
                increaseValue.setTextColor(color)
            }
            row.addView(percent)
            row.addView(increaseValue)
            row.setOnClickListener { v ->
                val group = v as ViewGroup
                val nameId = group.getChildAt(0) as ViewGroup
                val idText = nameId.getChildAt(1) as TextView
                if (SelectedStockItems_.contains(idText.text.toString())) {
                    v.setBackgroundColor(BackgroundColor_)
                    SelectedStockItems_.remove(idText.text.toString())
                } else {
                    v.setBackgroundColor(HighlightColor_)
                    SelectedStockItems_.add(idText.text.toString())
                }
            }
            table.addView(row)
            var sid = stock.id_
            sid = sid!!.replace("sh".toRegex(), "")
            sid = sid.replace("sz".toRegex(), "")
            var text = ""
            val sBuy = resources.getString(R.string.stock_buy)
            val sSell = resources.getString(R.string.stock_sell)
            if (stock.b1_!!.toDouble() >= StockLargeTrade_) {
                text += sBuy + "1:" + stock.b1_ + ","
            }
            if (stock.b2_!!.toDouble() >= StockLargeTrade_) {
                text += sBuy + "2:" + stock.b2_ + ","
            }
            if (stock.b3_!!.toDouble() >= StockLargeTrade_) {
                text += sBuy + "3:" + stock.b3_ + ","
            }
            if (stock.b4_!!.toDouble() >= StockLargeTrade_) {
                text += sBuy + "4:" + stock.b4_ + ","
            }
            if (stock.b5_!!.toDouble() >= StockLargeTrade_) {
                text += sBuy + "5:" + stock.b5_ + ","
            }
            if (stock.s1_!!.toDouble() >= StockLargeTrade_) {
                text += sSell + "1:" + stock.s1_ + ","
            }
            if (stock.s2_!!.toDouble() >= StockLargeTrade_) {
                text += sSell + "2:" + stock.s2_ + ","
            }
            if (stock.s3_!!.toDouble() >= StockLargeTrade_) {
                text += sSell + "3:" + stock.s3_ + ","
            }
            if (stock.s4_!!.toDouble() >= StockLargeTrade_) {
                text += sSell + "4:" + stock.s4_ + ","
            }
            if (stock.s5_!!.toDouble() >= StockLargeTrade_) {
                text += sSell + "5:" + stock.s5_ + ","
            }
            if (text.length > 0) sendNotifation(sid.toInt(), stock.name_, text)
        }
    }

    companion object {
        private val StockIds_: HashSet<String?> = HashSet<Any?>()
        private val SelectedStockItems_: Vector<String?> = Vector<Any?>()
        private const val BackgroundColor_ = Color.WHITE
        private val HighlightColor_ = Color.rgb(210, 233, 255)
        private const val ShIndex = "sh000001"
        private const val SzIndex = "sz399001"
        private const val ChuangIndex = "sz399006"
        private const val StockIdsKey_ = "StockIds"
        private const val StockLargeTrade_ = 1000000
    }
}