package vova.com.primegenerator

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.cell_result_number.view.*
import vova.com.primegenerator.data.NumbersDatabase
import vova.com.primegenerator.data.PrimeNumbersDao

class MainActivity : AppCompatActivity() {

    lateinit var adapter: MyAdapter
    lateinit var primeNumbersDao: PrimeNumbersDao
    var results: List<Int>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setListeners()

        savedInstanceState?.apply {
            results = getIntArray(KEY_RESULTS)?.run { toMutableList() }
            initRecyclerView()
            adapter.addData(results)

            limitEditText.setText(getString(KEY_LIMIT))
            resultTextView.text = getString(KEY_PRIMES_COUNT)
        }

        registerReceivers()
        primeNumbersDao = NumbersDatabase.getInstance(this).primeNumbersDao()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.apply {
            putIntArray(KEY_RESULTS, results?.toIntArray())
            putString(KEY_LIMIT, limitEditText.text.toString())
            putString(KEY_PRIMES_COUNT, resultTextView.text.toString())
        }
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(messageReceiver)
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(generationProgressReceiver)
        super.onDestroy()
    }

    private fun registerReceivers() {
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(
                messageReceiver,
                IntentFilter(ACTION_UPDATE_PRIMES_COUNT)
            )
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(
                generationProgressReceiver,
                IntentFilter(ACTION_UPDATE_PROGRESS_BAR)
            )
    }


    private val messageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val message = intent.getIntExtra(KEY_MESSAGE, 0)
            val values = intent.getIntArrayExtra(KEY_VALUES)

            resultTextView.text = message.toString()
            generationProgressBar.visible()

            adapter.addData(values.toMutableList())

        }
    }

    private val generationProgressReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val progress = intent.getBooleanExtra(KEY_PROGRESS, false)
            generationProgressBar.apply { if (progress) visible() else invisible() }
        }
    }

    private fun setListeners() {
        startButton.setOnClickListener {
            initRecyclerView()

            val intent = Intent(this, GeneratorService::class.java)
            intent.action = GeneratorService.ACTION_START_FOREGROUND_SERVICE
            intent.putExtra(KEY_LIMIT, limitEditText.text.toString().toInt())
            startService(intent)
        }
        limitEditText.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                startButton.apply { if (s.isNullOrBlank()) deactivate() else activate() }
            }

            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        })

    }

    private fun initRecyclerView() {
        adapter = MyAdapter(mutableListOf())

        FlexboxLayoutManager(this).apply {
            flexDirection = FlexDirection.ROW
            justifyContent = JustifyContent.SPACE_AROUND
            resultsRecyclerView.layoutManager = this
        }
        resultsRecyclerView.adapter = adapter
    }

    inner class MyAdapter(private val items: MutableList<Int>) :
        RecyclerView.Adapter<MyAdapter.ViewHolder>() {


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(parent.inflate(R.layout.cell_result_number))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            holder.bind(items[position])

        override fun getItemCount(): Int = items.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(item: Int) = with(itemView) {
                numberTextView.text = item.toString()
            }
        }

        fun addData(values: List<Int>?) {
            values?.let {
                items.addAll(it)
                results = items
                notifyDataSetChanged()
            }
        }
    }

    companion object {
        const val ACTION_UPDATE_PRIMES_COUNT = "action_update_primes_count"
        const val ACTION_UPDATE_PROGRESS_BAR = "action_update_progress_bar"
        const val KEY_MESSAGE = "message"
        const val KEY_VALUES = "values"
        const val KEY_PROGRESS = "progress"
        const val KEY_RESULTS = "results"
        const val KEY_LIMIT = "limit"
        const val KEY_PRIMES_COUNT = "primesCount"
    }

}
