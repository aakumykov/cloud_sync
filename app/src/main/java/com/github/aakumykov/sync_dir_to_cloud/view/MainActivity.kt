package com.github.aakumykov.sync_dir_to_cloud.view

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.OnBackStackChangedListener
import androidx.lifecycle.LiveData
import com.github.aakumykov.storage_access_helper.StorageAccessHelper
import com.github.aakumykov.sync_dir_to_cloud.R
import com.github.aakumykov.sync_dir_to_cloud.databinding.ActivityMainBinding
import com.github.aakumykov.sync_dir_to_cloud.utils.MyLogger
import com.github.aakumykov.sync_dir_to_cloud.view.common_view_models.PageTitleViewModel
import com.github.aakumykov.sync_dir_to_cloud.view.common_view_models.StorageAccessViewModel
import com.github.aakumykov.sync_dir_to_cloud.view.common_view_models.navigation.NavTarget
import com.github.aakumykov.sync_dir_to_cloud.view.common_view_models.navigation.NavigationViewModel
import com.github.aakumykov.sync_dir_to_cloud.view.other.menu_helper.CustomActions
import com.github.aakumykov.sync_dir_to_cloud.view.other.menu_helper.CustomMenuAction
import com.github.aakumykov.sync_dir_to_cloud.view.other.menu_helper.HasCustomActions
import com.github.aakumykov.sync_dir_to_cloud.view.other.menu_helper.MenuHelper
import com.github.aakumykov.sync_dir_to_cloud.view.task_edit.TaskEditFragment
import com.github.aakumykov.sync_dir_to_cloud.view.task_list.TaskListFragment
import com.github.aakumykov.sync_dir_to_cloud.view.task_state.TaskStateFragment

class MainActivity : AppCompatActivity() {

    private var currentCustomActionsLiveData: LiveData<CustomActions>? = null
    private var currentCustomActions: CustomActions? = null

    private lateinit var binding: ActivityMainBinding

    private val navigationViewModel: NavigationViewModel by viewModels()
    private val pageTitleViewModel: PageTitleViewModel by viewModels()

    private lateinit var fragmentManager: FragmentManager
    private var currentFragment: Fragment? = null
    private lateinit var onBackStackChangedListener: OnBackStackChangedListener
    private lateinit var fragmentLifecycleCallbacks: FragmentManager.FragmentLifecycleCallbacks

    private lateinit var storageAccessHelper: StorageAccessHelper
    private val storageAccessViewModel: StorageAccessViewModel by viewModels()

    private val menuHelper: MenuHelper by lazy { MenuHelper(this@MainActivity, R.color.onPrimary, R.color.primary) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        storageAccessHelper = StorageAccessHelper.create(this)
        storageAccessViewModel.storageAccessRequest.observe(this) {
            storageAccessHelper.requestStorageAccess {
                storageAccessViewModel.setStorageAccessResult(it)
            }
        }

        prepareView()
        prepareViewModels()
        prepareFragmentManager()
    }

    @SuppressLint("MissingSuperCall")
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        loadInitialFragment(intent)
    }


    override fun onDestroy() {
        super.onDestroy()
        releaseFragmentManager()
    }


    override fun onSupportNavigateUp(): Boolean {
        navigationViewModel.navigateBack()
        return true
    }

    private fun loadInitialFragment(intent: Intent?) {
        setFragment(
            when(intent?.action) {
                ACTION_SHOW_TASK_STATE -> TaskStateFragment.create(intent)
                else -> TaskListFragment.create()
            }
        )
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        updateMenu()
        return super.onCreateOptionsMenu(menu)
    }


    private fun prepareView() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
    }

    private fun prepareViewModels() {
        navigationViewModel.getNavigationTargetEvents().observe(this, this::onNewNavTarget)
        pageTitleViewModel.getPageTitle().observe(this, this::onPageTitleChanged)
    }

    private fun prepareFragmentManager() {

        fragmentManager = supportFragmentManager


        onBackStackChangedListener = OnBackStackChangedListener {
            if (0 == fragmentManager.backStackEntryCount) {
                supportActionBar?.setDisplayHomeAsUpEnabled(false)
            }
            else {
                supportActionBar?.apply {
                    setDisplayHomeAsUpEnabled(true)
                    setHomeAsUpIndicator(R.drawable.ic_page_back)
                }
            }
        }.also {
            fragmentManager.addOnBackStackChangedListener(it)
        }


        fragmentLifecycleCallbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
                super.onFragmentResumed(fm, f)
                currentFragment = f
//                updateMenu()
                subscribeToCustomActions()

                /*binding.toolbar.menu.add(R.string.MENU_ITEM_app_properties).apply {
                    setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                    setOnMenuItemClickListener { openAppProperties(); true }
                    ResourcesCompat.getDrawable(resources, R.drawable.ic_app_properties, theme).also {
                        it?.setTint(ResourcesCompat.getColor(resources, R.color.white, theme))
                        icon = it
                    }
                }*/
            }

            override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
                super.onFragmentPaused(fm, f)
                unsubscribeFromCustomActions()
            }
        }.also {
            fragmentManager.registerFragmentLifecycleCallbacks(it, false)
        }
    }

    private fun subscribeToCustomActions() {
//        MyLogger.d(TAG, "------------------------------")
//        MyLogger.d(TAG, "subscribeToCustomActions(${currentFragmentName()})")
        if (currentFragment is HasCustomActions) {
            currentCustomActionsLiveData = (currentFragment as HasCustomActions).customActions
            currentCustomActionsLiveData?.observe(this, ::onCustomActionsChanged)
        }
    }

    private fun unsubscribeFromCustomActions() {
//        MyLogger.d(TAG, "unsubscribeFromCustomActions(${currentFragmentName()})")
        currentCustomActionsLiveData?.removeObservers(this)
    }

    private fun onCustomActionsChanged(customMenuActions: Array<CustomMenuAction>?) {
//        MyLogger.d(TAG, "onCustomActionsChanged(count: ${customMenuActions?.size}), ${currentFragmentName()}")
        customMenuActions?.also {
            currentCustomActions = it
            updateMenu()
        }
    }

    private fun currentFragmentName(): String? = currentFragment?.javaClass?.simpleName


    private fun releaseFragmentManager() {
        fragmentManager.removeOnBackStackChangedListener(onBackStackChangedListener)
        fragmentManager.unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks)
    }



    private fun onPageTitleChanged(title: String) {
        setTitle(title)
    }

    private fun onNewNavTarget(navTarget: NavTarget) {
        when (navTarget) {
            is NavTarget.Add -> loadFragment(TaskEditFragment.create())
            is NavTarget.Edit -> loadFragment(TaskEditFragment.create(navTarget.id))
            is NavTarget.Back -> returnToPrevFragment()
            is NavTarget.TaskInfo -> loadFragment(TaskStateFragment.create(navTarget.id))
            else -> loadInitialFragment(intent)
        }
    }

    private fun setFragment(fragment: Fragment) {

        fragmentManager.clearBackStack(DEFAULT_BACK_STACK_NAME)

        fragmentManager.beginTransaction()
            .setReorderingAllowed(false)
            .replace(R.id.fragmentContainerView, fragment, null)
            .commitNow()
    }

    private fun loadFragment(fragment: Fragment) {
        fragmentManager.beginTransaction()
            .addToBackStack(DEFAULT_BACK_STACK_NAME)
            .setReorderingAllowed(true)
            .replace(R.id.fragmentContainerView, fragment)
            .commit()
    }

    private fun returnToPrevFragment() {
        fragmentManager.popBackStack()
    }

    private fun updateMenu() {
//        MyLogger.d(TAG, "updateMenu(), ${currentFragmentName()}")
        binding.toolbar.menu.apply {
            clear()
            menuHelper.generateMenu(this, currentCustomActions)
        }
    }


    companion object {
        val TAG: String = MainActivity::class.java.simpleName

        const val ACTION_SHOW_TASK_STATE: String = "SHOW_TASK_STATE"

        private const val DEFAULT_BACK_STACK_NAME = "default_back_stack"
    }
}