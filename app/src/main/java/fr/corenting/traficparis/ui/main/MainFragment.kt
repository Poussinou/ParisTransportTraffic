package fr.corenting.traficparis.ui.main

import android.content.Context
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.*
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.itemanimators.SlideInOutLeftAnimator
import fr.corenting.traficparis.BuildConfig
import fr.corenting.traficparis.R
import fr.corenting.traficparis.models.ApiResponseResults
import fr.corenting.traficparis.traffic.TrafficViewModel
import fr.corenting.traficparis.utils.MiscUtils
import fr.corenting.traficparis.utils.PersistenceUtils
import fr.corenting.traficparis.utils.ResultsUtils
import kotlinx.android.synthetic.main.main_fragment.*


class MainFragment : androidx.fragment.app.Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private var menu: Menu? = null
    private lateinit var viewModel: TrafficViewModel
    private lateinit var observer: Observer<ApiResponseResults>

    private var displayRer = true
    private var displayMetro = true
    private var displayTram = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        // Observable for refresh
        observer = Observer {
            if (it == null) {
                endLoading(empty = true)
                Snackbar.make(
                        container, getString(R.string.download_error),
                        Snackbar.LENGTH_SHORT
                ).show()
            } else {
                endLoading(empty = false)
                recyclerView.layoutManager = LinearLayoutManager(context)

                // Apply filter
                val filteredResults = ResultsUtils.filterResults(
                        it, displayRer, displayMetro, displayTram
                )
                (recyclerView.adapter as MainAdapter)
                        .addItems(ResultsUtils.convertApiResultsToListItems(filteredResults))
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }


    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.fragment_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)

        // Update menu values for filters from shared prefs
        if (context != null) {
            displayRer = PersistenceUtils.getDisplayRerValue(activity as Context)
            displayMetro = PersistenceUtils.getDisplayMetroValue(activity as Context)
            displayTram = PersistenceUtils.getDisplayTramValue(activity as Context)

            val subMenu = menu?.getItem(0)?.subMenu
            subMenu?.getItem(0)?.isChecked = displayRer
            subMenu?.getItem(1)?.isChecked = displayMetro
            subMenu?.getItem(2)?.isChecked = displayTram

        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null) {
            when {
                item.itemId == R.id.about_menu -> showAboutPopup()
                item.itemId == R.id.filter_rer ||
                        item.itemId == R.id.filter_metro ||
                        item.itemId == R.id.filter_tram -> {
                    item.isChecked = !item.isChecked
                    PersistenceUtils.setValue(activity as Context, item.itemId, item.isChecked)
                    changeDisplayedCategories(item.itemId, item.isChecked)
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Init recyclerview
        recyclerView.itemAnimator = SlideInOutLeftAnimator(recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = MainAdapter(context!!)

        startLoading()

        // View model
        viewModel = ViewModelProviders.of(this).get(TrafficViewModel::class.java)

        // Add listeners
        val listener = {
            startLoading()
            viewModel.getTraffic().observe(this, observer)
        }
        emptySwipeRefreshLayout.setOnRefreshListener(listener)
        swipeRefreshLayout.setOnRefreshListener(listener)

        // Load data
        viewModel.getTraffic().observe(this, observer)
    }

    private fun changeDisplayedCategories(filterId: Int, newValue: Boolean) {
        when (filterId) {
            R.id.filter_rer -> displayRer = newValue
            R.id.filter_metro -> displayMetro = newValue
            R.id.filter_tram -> displayTram = newValue
        }

        // Refresh the display
        startLoading()
        viewModel.getTraffic().observe(this, observer)
    }

    private fun showAboutPopup() {
        (android.app.AlertDialog.Builder(context)
                .setTitle(R.string.app_name)
                .setIcon(R.mipmap.ic_launcher)
                .setMessage(MiscUtils.htmlToSpanned(getString(R.string.about_text) + BuildConfig.VERSION_NAME))
                .setNegativeButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
                .findViewById(android.R.id.message) as TextView).movementMethod = LinkMovementMethod.getInstance()
    }

    private fun endLoading(empty: Boolean) {
        emptySwipeRefreshLayout.post {
            emptySwipeRefreshLayout.visibility = if (empty) View.VISIBLE else View.GONE
            emptySwipeRefreshLayout.isRefreshing = false
        }
        swipeRefreshLayout.post {
            swipeRefreshLayout.visibility = if (empty) View.GONE else View.VISIBLE
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun startLoading() {
        (recyclerView.adapter as MainAdapter).removeAllItems()
        emptySwipeRefreshLayout.post { emptySwipeRefreshLayout.visibility = View.GONE }
        swipeRefreshLayout.post {
            swipeRefreshLayout.visibility = View.VISIBLE
            swipeRefreshLayout.isRefreshing = true
        }
    }
}
