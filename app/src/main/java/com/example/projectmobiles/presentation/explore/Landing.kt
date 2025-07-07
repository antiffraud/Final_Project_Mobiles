package com.example.projectmobiles.presentation.explore

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import com.example.projectmobiles.R
import com.example.projectmobiles.presentation.bookmark.favorite
import com.example.projectmobiles.presentation.profile.profile
import com.example.projectmobiles.presentation.cart.shop
import com.google.android.material.bottomnavigation.BottomNavigationView


class landing : AppCompatActivity() {

    private lateinit var navBar: BottomNavigationView
    private val iconMap = mapOf(
        R.id.nav_explore to Pair(R.drawable.ic_explore_click, R.drawable.ic_explore),
        R.id.nav_favorite to Pair(R.drawable.ic_bookmark_click, R.drawable.ic_bookmark),
        R.id.nav_cart to Pair(R.drawable.ic_cart_click, R.drawable.ic_cart),
        R.id.nav_profile to Pair(R.drawable.ic_profile_click, R.drawable.ic_profile)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing)

        navBar = findViewById(R.id.bottomNavigationView)
        navBar.post {
            val selectedId = navBar.selectedItemId
            navBar.menu.forEach { menuItem ->
                val (selectedIcon, unselectedIcon) = iconMap[menuItem.itemId] ?: return@forEach
                menuItem.icon = ContextCompat.getDrawable(
                    this,
                    if (menuItem.itemId == selectedId) selectedIcon else unselectedIcon
                )
            }
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, explore())
            .commit()

        navBar.setOnItemSelectedListener { item ->
            navBar.menu.forEach { menuItem ->
                val (selectedIcon, unselectedIcon) = iconMap[menuItem.itemId] ?: return@forEach
                menuItem.icon = ContextCompat.getDrawable(
                    this,
                    if (menuItem.itemId == item.itemId) selectedIcon else unselectedIcon
                )
            }

            when (item.itemId) {
                R.id.nav_explore -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, explore())
                        .commit()
                    true
                }
                R.id.nav_favorite -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, favorite())
                        .commit()
                    true
                }
                R.id.nav_cart -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, shop())
                        .commit()
                    true
                }
                R.id.nav_profile -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, profile())
                        .commit()
                    true
                }
                else -> false
            }
        }
    }
}

