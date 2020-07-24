/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.kiwix.kiwixmobile.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.Toolbar
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.activity_new_navigation.bottom_nav_view
import kotlinx.android.synthetic.main.activity_new_navigation.container
import kotlinx.android.synthetic.main.activity_new_navigation.drawer_nav_view
import kotlinx.android.synthetic.main.activity_new_navigation.reader_drawer_nav_view
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.Intents
import org.kiwix.kiwixmobile.core.base.BaseFragmentActivityExtensions
import org.kiwix.kiwixmobile.core.di.components.CoreComponent
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.intent
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.start
import org.kiwix.kiwixmobile.core.help.HelpActivity
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.page.bookmark.BookmarksActivity
import org.kiwix.kiwixmobile.core.page.history.HistoryActivity
import org.kiwix.kiwixmobile.core.settings.CoreSettingsActivity
import org.kiwix.kiwixmobile.core.utils.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.EXTRA_EXTERNAL_LINK
import org.kiwix.kiwixmobile.core.utils.KiwixDialog
import org.kiwix.kiwixmobile.core.utils.REQUEST_HISTORY_ITEM_CHOSEN
import org.kiwix.kiwixmobile.core.utils.REQUEST_PREFERENCES
import org.kiwix.kiwixmobile.kiwixActivityComponent
import org.kiwix.kiwixmobile.webserver.ZimHostActivity
import javax.inject.Inject

class KiwixNewNavigationActivity : CoreMainActivity(),
  NavigationView.OnNavigationItemSelectedListener {
  private lateinit var navController: NavController
  private lateinit var appBarConfiguration: AppBarConfiguration
  private lateinit var drawerToggle: ActionBarDrawerToggle
  private var actionMode: ActionMode? = null
  @Inject lateinit var alertDialogShower: AlertDialogShower

  override fun injection(coreComponent: CoreComponent) {
    kiwixActivityComponent.inject(this)
  }

  private val finishActionModeOnDestinationChange =
    NavController.OnDestinationChangedListener { controller, destination, arguments ->
      actionMode?.finish()
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_new_navigation)

    navController = findNavController(R.id.nav_host_fragment)
    navController.addOnDestinationChangedListener(finishActionModeOnDestinationChange)
    appBarConfiguration = AppBarConfiguration(
      setOf(
        R.id.navigation_downloads,
        R.id.navigation_library,
        R.id.navigation_reader
      ), container
    )
    drawer_nav_view.setupWithNavController(navController)
    drawer_nav_view.setNavigationItemSelectedListener(this)
    bottom_nav_view.setupWithNavController(navController)
  }

  override fun onSupportActionModeStarted(mode: ActionMode) {
    super.onSupportActionModeStarted(mode)
    actionMode = mode
  }

  fun setupDrawerToggle(toolbar: Toolbar) {
    drawerToggle =
      ActionBarDrawerToggle(
        this, container, toolbar, R.string.open, R.string.close_all_tabs
      )
    container.addDrawerListener(drawerToggle)
    drawerToggle.isDrawerIndicatorEnabled = true
    drawerToggle.syncState()
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (drawerToggle.onOptionsItemSelected(item)) {
      return true
    }
    return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
  }

  override fun onSupportNavigateUp(): Boolean {
    val navController = findNavController(R.id.nav_host_fragment)
    return navController.navigateUp() ||
      super.onSupportNavigateUp()
  }

  override fun onBackPressed() {
    if (container.isDrawerOpen(drawer_nav_view)) {
      container.closeDrawer(drawer_nav_view)
      return
    } else if (container.isDrawerOpen(reader_drawer_nav_view)) {
      container.closeDrawer(reader_drawer_nav_view)
      return
    }
    supportFragmentManager.fragments.filterIsInstance<BaseFragmentActivityExtensions>().forEach {
      if (it.onBackPressed(this) == BaseFragmentActivityExtensions.Super.ShouldCall) {
        super.onBackPressed()
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    supportFragmentManager.fragments.filterIsInstance<BaseFragmentActivityExtensions>().forEach {
      it.onNewIntent(intent, this)
    }
  }

  override fun onNavigationItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.menu_support_kiwix -> openSupportKiwixExternalLink()
      R.id.menu_settings -> openSettingsActivity()
      R.id.menu_help -> start<HelpActivity>()
      R.id.menu_host_books -> start<ZimHostActivity>()
      R.id.menu_history -> openHistoryActivity()
      R.id.menu_bookmarks_list -> openBookmarksActivity()
      else -> return false
    }
    return true
  }

  private fun openSupportKiwixExternalLink() {
    val intent = Intent(
      Intent.ACTION_VIEW,
      Uri.parse("https://www.kiwix.org/support")
    ).putExtra(EXTRA_EXTERNAL_LINK, true)
    alertDialogShower.show(KiwixDialog.ExternalLinkPopup,
      { startActivity(intent) }, {
        sharedPreferenceUtil.putPrefExternalLinkPopup(false)
        startActivity(intent)
      }
    )
  }

  private fun openSettingsActivity() {
    startActivityForResult(
      Intents.internal(CoreSettingsActivity::class.java),
      REQUEST_PREFERENCES
    )
  }

  private fun openHistoryActivity() {
    startActivityForResult(
      intent<HistoryActivity>(),
      REQUEST_HISTORY_ITEM_CHOSEN
    )
  }

  private fun openBookmarksActivity() {
    startActivity(
      intent<BookmarksActivity>()
    )
  }
}
