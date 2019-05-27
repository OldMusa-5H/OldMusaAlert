package com.cnr_isac.oldmusa


import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.cnr_isac.oldmusa.Account.isAdmin
import com.cnr_isac.oldmusa.util.ApiUtil.api
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)

        setSupportActionBar(toolbar)

        val navController = findNavController(R.id.nav_host_fragment)
        AppBarConfiguration(navController.graph)
        findViewById<NavigationView>(R.id.nav_view).setupWithNavController(navController)


        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener(this)

        // Set navigation username

        val username: TextView = navView.getHeaderView(0).findViewById(R.id.username)
        username.text = intent.getStringExtra("username") ?: "Username"

        Account.resetAdminCache(intent.getCharExtra("permission", '?') == 'A')


        navView.menu.findItem(R.id.manage_users).isVisible = isAdmin
        navView.menu.findItem(R.id.current_user_detail).isVisible = !isAdmin

    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)

        when (item.itemId) {
            R.id.home -> {
                navController.popBackStack(R.id.home, false)
            }
            R.id.logout -> {
                api.logout()
                Account.saveToken(this)

                val intent = Intent(this, Login::class.java)
                finish()
                startActivity(intent)
            }
            R.id.manage_users -> {
                navController.navigate(R.id.manageUsers)
            }
            R.id.current_user_detail -> {
                navController.navigate(R.id.userDetailsEdit, UserDetailsEditArgs(api.getMe().id).toBundle())
            }
            R.id.about -> {
                navController.navigate(R.id.about)
            }
        }

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

}
