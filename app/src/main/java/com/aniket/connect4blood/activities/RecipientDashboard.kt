package com.aniket.connect4blood.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.Fragment
import com.aniket.connect4blood.R
import com.aniket.connect4blood.fragments.RecipientDashboardHistory
import com.aniket.connect4blood.fragments.RecipientDashboardHome
import com.aniket.connect4blood.fragments.RecipientDashboardSettings
import com.google.android.material.bottomnavigation.BottomNavigationView

class RecipientDashboard : AppCompatActivity() {

    private lateinit var dashboardBottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipient_dashboard)

        dashboardBottomNav = findViewById(R.id.dashboardBottomNav)
        dashboardBottomNav.setOnItemSelectedListener {
            supportFragmentManager.popBackStack()
            when (it.itemId) {
                R.id.bottomNavHome -> {
                    openFragment(
                        R.id.dashboardContent,
                        RecipientDashboardHome()
                    )
                    true
                }
                R.id.bottomNavRequests -> {
                    openFragment(
                        R.id.dashboardContent,
                        RecipientDashboardHistory()
                    )
                    true
                }
                R.id.bottomNavSettings -> {
                    openFragment(
                        R.id.dashboardContent,
                        RecipientDashboardSettings()
                    )
                    true
                }
                else -> false
            }
        }

        openFragment(R.id.dashboardContent, RecipientDashboardHome())
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                closeFragment()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

     fun closeFragment() {
        if(supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStackImmediate()
        }
    }

    private fun openFragment(layoutId: Int, fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(layoutId, fragment)
            .commit()
    }
}