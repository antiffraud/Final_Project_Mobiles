package com.example.projectmobiles.domain.usecase

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import com.example.projectmobiles.R

object DotIndicatorUtil {
    fun updateMovingDots(context: Context, container: LinearLayout, selectedIndex: Int) {
        val totalDots = 3 // tetap 3 saja
        container.removeAllViews()

        for (i in 0 until totalDots) {
            val dot = View(context)
            val width = if (i == selectedIndex % totalDots) 24 else 8
            val height = 8
            val params = LinearLayout.LayoutParams(width, height)
            params.marginStart = 8
            dot.layoutParams = params
            dot.setBackgroundResource(
                if (i == selectedIndex % totalDots) R.drawable.dot_selected else R.drawable.dot_unselected
            )
            container.addView(dot)
        }
    }

}
