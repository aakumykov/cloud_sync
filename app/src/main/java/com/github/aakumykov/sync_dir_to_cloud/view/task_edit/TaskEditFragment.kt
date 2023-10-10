package com.github.aakumykov.sync_dir_to_cloud.view.task_edit

import android.os.Bundle
import android.text.format.DateFormat.is24HourFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.github.aakumykov.sync_dir_to_cloud.R
import com.github.aakumykov.sync_dir_to_cloud.databinding.FragmentTaskEditBinding
import com.github.aakumykov.sync_dir_to_cloud.domain.entities.SyncTask
import com.github.aakumykov.sync_dir_to_cloud.view.common_view_models.PageTitleViewModel
import com.github.aakumykov.sync_dir_to_cloud.view.common_view_models.navigation.NavTarget
import com.github.aakumykov.sync_dir_to_cloud.view.common_view_models.navigation.NavigationViewModel
import com.github.aakumykov.sync_dir_to_cloud.view.common_view_models.op_state.OpState
import com.github.aakumykov.sync_dir_to_cloud.view.view_utils.TextMessage
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat

class TaskEditFragment : Fragment() {

    private var _binding: FragmentTaskEditBinding? = null
    private val binding get() = _binding!!

    private val taskEditViewModel: TaskEditViewModel by viewModels()
    private val navigationViewModel: NavigationViewModel by activityViewModels()
    private val pageTitleViewModel: PageTitleViewModel by activityViewModels()

    private var firstRun: Boolean = true
    private lateinit var currentTask: SyncTask


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        firstRun = null == savedInstanceState
        prepareLayout(inflater, container)
        prepareButtons()
        prepareViewModels()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val taskId: String? = arguments?.getString(TASK_ID)

        pageTitleViewModel.setPageTitle(getString(
            when(taskId) {
                null -> R.string.FRAGMENT_TASK_EDIT_creation_title
                else -> R.string.FRAGMENT_TASK_EDIT_edition_title
            }
        ))

        if (null != taskId)
            taskEditViewModel.loadTask(taskId)
    }

    override fun onStop() {
        super.onStop()
        setFormValuesToCurrentTask()
        taskEditViewModel.storeCurrentTask(currentTask)
    }

    private fun setFormValuesToCurrentTask() {
        currentTask.sourcePath = binding.sourcePathInput.text.toString()
        currentTask.targetPath = binding.targetPathInput.text.toString()
//        currentTask.intervalMinutes
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }



    private fun prepareLayout(inflater: LayoutInflater, container: ViewGroup?) {
        _binding = FragmentTaskEditBinding.inflate(inflater, container, false)
    }

    private fun prepareButtons() {
        binding.periodSelectionButton.setOnClickListener { onTimeButtonClicked() }
        binding.saveButton.setOnClickListener { onSaveButtonClicked() }
        binding.cancelButton.setOnClickListener { onCancelButtonClicked() }
    }

    private fun prepareViewModels() {
        taskEditViewModel.getCurrentTask().observe(viewLifecycleOwner, this::onCurrentTaskChanged)
        taskEditViewModel.getOpState().observe(viewLifecycleOwner, this::onOpStateChanged)
    }


    private fun onCurrentTaskChanged(syncTask: SyncTask) {
        currentTask = syncTask
        fillForm()
    }

    private fun onOpStateChanged(opState: OpState) {
        when (opState) {
            is OpState.Idle -> showIdleOpState()
            is OpState.Busy -> showBusyOpState(opState)
            is OpState.Success -> finishWork(opState)
            is OpState.Error -> showErrorOpState(opState)
        }
    }


    private fun onSaveButtonClicked() {
        taskEditViewModel.createOrSaveSyncTask2()
    }

    private fun onCancelButtonClicked() {
        navigationViewModel.navigateTo(NavTarget.Back)
    }

    private fun onTimeButtonClicked() {

        val isSystem24Hour = is24HourFormat(requireContext())
        val clockFormat = if (isSystem24Hour) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H

        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(clockFormat)
            .setHour(12)
            .setMinute(10)
            .setTitleText(R.string.sync_task_regulatiry_picker_title)
            .build()

        picker.addOnPositiveButtonClickListener {
            fillPeriodView(picker.hour, picker.minute)
        }

        picker.showNow(childFragmentManager, "")
    }

    private fun finishWork(successOpState: OpState.Success) {
        Toast.makeText(requireContext(), successOpState.textMessage.get(requireContext()), Toast.LENGTH_SHORT).show()
        navigationViewModel.navigateBack()
    }



    private fun fillForm() {
        if (firstRun) {
            binding.sourcePathInput.setText(currentTask.sourcePath)
            binding.targetPathInput.setText(currentTask.targetPath)

            fillPeriodView()
        }
    }

    private fun fillPeriodView() {
        fillPeriodView(currentTask.intervalHours, currentTask.intervalMinutes)
    }

    private fun fillPeriodView(hourNumber: Int, minuteNumber: Int) {

        val h = if (0 == hourNumber) 24 else hourNumber
        val m = minuteNumber

        val hPluralName = requireContext().resources.getQuantityString(R.plurals.hours, h)
        val mPluralName = requireContext().resources.getQuantityString(R.plurals.minutes, m)

        binding.regularityInput.setText(getString(
            R.string.sync_task_interval_view,
            h, hPluralName,
            m, mPluralName
        ))
    }



    private fun showIdleOpState() {
        hideProgressBar()
        hideProgressMessage()
        enableForm()
        hideErrorMessage()
    }

    private fun showBusyOpState(opState: OpState.Busy) {
        showProgressBar()
        showProgressMessage(opState.textMessage)
        disableForm()
        hideErrorMessage()
    }

    private fun showErrorOpState(opState: OpState.Error) {
        hideProgressBar()
        hideProgressMessage()
        enableForm()
        showErrorMessage(opState.errorMessage)
    }


    private fun showProgressMessage(textMessage: TextMessage) {
        binding.progressMessage.text = textMessage.get(requireContext())
        binding.progressMessage.visibility = View.VISIBLE
    }

    private fun hideProgressMessage() {
        binding.progressMessage.text = ""
        binding.progressMessage.visibility = View.GONE
    }


    private fun showProgressBar() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        binding.progressBar.visibility = View.GONE
    }


    private fun enableForm() {
        binding.sourcePathInput.isEnabled = true
        binding.targetPathInput.isEnabled = true

        binding.sourcePathSelectionButton.isEnabled = true
        binding.targetPathSelectionButton.isEnabled = true

        binding.saveButton.isEnabled = true
    }

    private fun disableForm() {
        binding.sourcePathInput.isEnabled = false
        binding.targetPathInput.isEnabled = false

        binding.sourcePathSelectionButton.isEnabled = false
        binding.targetPathSelectionButton.isEnabled = false

        binding.saveButton.isEnabled = false
    }


    private fun showErrorMessage(errorMessage: TextMessage) {
        binding.errorMessage.text = errorMessage.get(requireContext())
        binding.errorMessage.visibility = View.VISIBLE
    }

    private fun hideErrorMessage() {
        binding.errorMessage.visibility = View.GONE
    }


    companion object {

        private const val TASK_ID = "TASK_ID"
        private const val REGULARITY_HOURS = "REGULARITY_HOURS"
        private const val REGULARITY_MINUTES = "REGULARITY_MINUTES"

        fun create(): TaskEditFragment =
            createFragment(null)

        fun create(taskId: String): TaskEditFragment =
            createFragment(taskId)

        private fun createFragment(taskId: String?) = TaskEditFragment().apply {
                arguments = bundleOf(TASK_ID to taskId)
        }
    }
}