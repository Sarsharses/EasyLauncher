package com.github.droidworksstudio.launcher.ui.drawer

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.github.droidworksstudio.launcher.R
import com.github.droidworksstudio.launcher.data.entities.AppInfo
import com.github.droidworksstudio.launcher.databinding.FragmentDrawBinding
import com.github.droidworksstudio.launcher.helper.AppHelper
import com.github.droidworksstudio.launcher.helper.FingerprintHelper
import com.github.droidworksstudio.launcher.listener.OnItemClickedListener
import com.github.droidworksstudio.launcher.ui.bottomsheetdialog.AppInfoBottomSheetFragment
import com.github.droidworksstudio.launcher.viewmodel.AppViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
@AndroidEntryPoint
class DrawFragment : Fragment(), OnItemClickedListener.OnAppsClickedListener,
    OnItemClickedListener.OnAppLongClickedListener,
    OnItemClickedListener.BottomSheetDismissListener,
    OnItemClickedListener.OnAppStateClickListener,
    FingerprintHelper.Callback{
    private var _binding: FragmentDrawBinding? = null

    private val binding get() = _binding!!

    private val viewModel: AppViewModel by viewModels()

    private val drawAdapter: DrawAdapter by lazy { DrawAdapter(this, this) }

    @Inject
    lateinit var appHelper: AppHelper

    @Inject
    lateinit var fingerHelper: FingerprintHelper

    private lateinit var context: Context
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentDrawBinding.inflate(inflater, container, false)

        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        appHelper.dayNightMod(requireContext(), binding.drawBackground)
        super.onViewCreated(view, savedInstanceState)

        context = requireContext()

        setupRecyclerView()
        setupSearch()
        observeClickListener()
    }

    private fun setupRecyclerView() {

        binding.drawAdapter.apply {
            adapter = drawAdapter
            layoutManager = StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL)
            setHasFixedSize(false)
        }
    }

    private fun observeDrawerApps() {
        viewModel.compareInstalledAppInfo()

        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            viewModel.drawApps.collect{
                drawAdapter.submitList(it)
                drawAdapter.updateDataWithStateFlow(it)
            }
        }
    }

    private fun setupSearch() {
        binding.searchView1.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Do Nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchApp(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {
                // Do Nothing
            }
        })
    }

    private fun observeClickListener(){
        binding.drawSearchButton.setOnClickListener {appHelper.showSoftKeyboard(context, binding.searchView1)}
    }

    private fun searchApp(query: String?) {

        val searchQuery = "%$query%"
        viewLifecycleOwner.lifecycle.coroutineScope.launchWhenCreated {
            viewModel.searchAppInfo(searchQuery).collect { drawAdapter.submitList(it) }
        }
    }

    private fun showSelectedApp(appInfo: AppInfo) {
        binding.searchView1.text?.clear()

        val bottomSheetFragment = AppInfoBottomSheetFragment(appInfo)
        bottomSheetFragment.setOnBottomSheetDismissedListener(this)
        bottomSheetFragment.setOnAppStateClickListener(this)
        bottomSheetFragment.show(parentFragmentManager, "BottomSheetDialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onPause() {
        super.onPause()
        binding.searchView1.text?.clear()
    }

    override fun onResume() {
        super.onResume()
        observeDrawerApps()
        binding.drawAdapter.scrollToPosition(0)
        appHelper.hideKeyboard(context, binding.searchView1)
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onAppClicked(appInfo: AppInfo) {
        observeBioAuthCheck(appInfo)
    }

    override fun onAppLongClicked(appInfo: AppInfo) {
        showSelectedApp(appInfo)
    }

    override fun onBottomSheetDismissed() {
    }

    override fun onAppStateClicked(appInfo: AppInfo) {
        viewModel.update(appInfo)
        Log.d("Tag", "${appInfo.appName} : Draw Favorite: ${appInfo.favorite}")
    }

    private fun observeBioAuthCheck(appInfo: AppInfo) {
        if (!appInfo.lock) {
            appHelper.launchApp(context, appInfo)
        } else {
            fingerHelper.startFingerprintAuth(appInfo,this)
        }
    }

    override fun onAuthenticationSucceeded(appInfo: AppInfo) {
        Toast.makeText(context, getString(R.string.authentication_succeeded), Toast.LENGTH_SHORT)
            .show()
        appHelper.launchApp(context, appInfo)
    }

    override fun onAuthenticationFailed() {
        Toast.makeText(context, getString(R.string.authentication_failed), Toast.LENGTH_SHORT)
            .show()
    }

    override fun onAuthenticationError(errorCode: Int, errorMessage: CharSequence?) {
        Toast.makeText(context, getString(R.string.authentication_error), Toast.LENGTH_SHORT)
            .show()
    }
}

