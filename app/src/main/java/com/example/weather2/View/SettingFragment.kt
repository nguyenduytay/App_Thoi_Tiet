package com.example.weather2.View

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.weather2.R
import com.example.weather2.databinding.FragmentSettingBinding
import com.example.weather2.databinding.FragmentSystemBinding

class SettingFragment : Fragment() {
    private lateinit var bindingSetting: FragmentSettingBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetting=FragmentSettingBinding.inflate(inflater,container,false)
        return bindingSetting.root
    }
}