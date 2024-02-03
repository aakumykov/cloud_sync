package com.github.aakumykov.sync_dir_to_cloud.view.task_info

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.github.aakumykov.sync_dir_to_cloud.DaggerViewModelHelper
import com.github.aakumykov.sync_dir_to_cloud.R
import com.github.aakumykov.sync_dir_to_cloud.databinding.FragmentTaskStateBinding
import com.github.aakumykov.sync_dir_to_cloud.domain.entities.SyncObject
import com.github.aakumykov.sync_dir_to_cloud.domain.entities.SyncTask
import com.github.aakumykov.sync_dir_to_cloud.view.common_view_models.PageTitleViewModel
import com.github.aakumykov.sync_dir_to_cloud.view.common_view_models.navigation.NavigationViewModel
import com.github.aakumykov.sync_dir_to_cloud.view.ext_functions.showToast
import com.github.aakumykov.sync_dir_to_cloud.view.utils.ListViewAdapter
import kotlinx.coroutines.launch

class TaskStateFragment : Fragment(R.layout.fragment_task_state) {

    private var _binding: FragmentTaskStateBinding? = null
    private val binding get() = _binding!!

    // TODO: внудрять ViewModel-и Dagger-ом?
    private lateinit var mTaskInfoViewModel: TaskInfoViewModel // эту получаю от Dagger-а
    private val navigationViewModel: NavigationViewModel by activityViewModels()
    private val pageTitleViewModel: PageTitleViewModel by activityViewModels()

    private lateinit var listAdapter: ListViewAdapter<SyncObject>
    private val syncObjectList: MutableList<SyncObject> = mutableListOf()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pageTitleViewModel.setPageTitle(getString(R.string.FRAGMENT_TASK_INFO_title))

        val taskId: String? = arguments?.getString(KEY_TASK_ID)
        if (null == taskId) {
            showToast(R.string.there_is_no_task_id)
            navigationViewModel.navigateBack()
            return
        }

        _binding = FragmentTaskStateBinding.bind(view)

        listAdapter = ListViewAdapter(requireContext(), R.layout.sync_object_list_item, R.id.title, syncObjectList)
        binding.listView.adapter = listAdapter

        mTaskInfoViewModel = DaggerViewModelHelper.get(this, TaskInfoViewModel::class.java)

        lifecycleScope.launch {
            mTaskInfoViewModel.getSyncObjectList(taskId).observe(viewLifecycleOwner) { list ->
                syncObjectList.clear()
                syncObjectList.addAll(list)
                listAdapter.notifyDataSetChanged()
            }

            mTaskInfoViewModel.getSyncTask(taskId).observe(viewLifecycleOwner, ::displaySyncTask)
        }
    }

    private fun displaySyncTask(syncTask: SyncTask?) {
        if (null == syncTask)
            return

        binding.titleView.text = "${syncTask.sourcePath} --> ${syncTask.targetPath}"
        binding.idView.text = "(${syncTask.id})"

        displaySchedulingState(syncTask)
        displayExecutionState(syncTask)
    }

    private fun displaySchedulingState(syncTask: SyncTask) {
        binding.schedulingStateView.text =
            if (syncTask.isEnabled) {
                when(syncTask.schedulingState) {
                    SyncTask.SimpleState.IDLE -> detailedSchedulingState(syncTask)
                    SyncTask.SimpleState.BUSY -> getString(R.string.SCHEDULING_STATE_scheduling_now)
                    SyncTask.SimpleState.ERROR -> getString(R.string.SCHEDULING_STATE_scheduling_error, syncTask.schedulingError)
                }
            }
            else {
                getString(R.string.SCHEDULING_STATE_disabled)
            }
    }

    private fun detailedSchedulingState(syncTask: SyncTask): String {
        return when (syncTask.intervalHours) {
            0 -> getString(
                R.string.SCHEDULING_STATE_scheduled_short_form,
                syncTask.intervalMinutes,
                resources.getQuantityString(R.plurals.minutes, syncTask.intervalMinutes))

            else ->getString(
                R.string.SCHEDULING_STATE_scheduled_long_form,
                syncTask.intervalHours,
                syncTask.intervalMinutes,
                resources.getQuantityString(R.plurals.hours, syncTask.intervalHours),
                resources.getQuantityString(R.plurals.minutes, syncTask.intervalMinutes))
        }
    }

    private fun displayExecutionState(syncTask: SyncTask) {
        if (syncTask.isEnabled) {
            binding.executionStateView.text = when (syncTask.executionState) {
                SyncTask.SimpleState.IDLE -> getString(R.string.EXECUTION_STATE_idle)
                SyncTask.SimpleState.BUSY -> getString(R.string.EXECUTION_STATE_running)
                SyncTask.SimpleState.ERROR -> getString(
                    R.string.EXECUTION_STATE_error,
                    syncTask.executionError
                )
            }
            binding.executionStateView.visibility = View.VISIBLE
        }
        else {
            binding.executionStateView.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    companion object {

        const val KEY_TASK_ID = "TASK_ID"

        fun create(taskId: String?): TaskStateFragment {
            return TaskStateFragment().apply {
                arguments = Bundle().apply { putString(KEY_TASK_ID, taskId) }
            }
        }

        fun create(intent: Intent): TaskStateFragment {
            return create(intent.getStringExtra(KEY_TASK_ID))
        }
    }
}