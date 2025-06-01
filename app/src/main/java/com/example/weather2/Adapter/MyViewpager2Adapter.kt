package com.example.weather2.Adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.weather2.View.ChartFragment
import com.example.weather2.View.SettingFragment
import com.example.weather2.View.SystemFragment
import com.example.weather2.View.WeatherFragment

 class MyViewpager2Adapter(activity: FragmentActivity) :
    FragmentStateAdapter(activity) {
    override fun getItemCount(): Int =4

    override fun createFragment(position: Int): Fragment {
       return when (position)
       {
           0 -> WeatherFragment()
           1 -> SystemFragment()
           2 -> ChartFragment()
           3 -> SettingFragment()
           else -> SystemFragment()
       }
    }
}