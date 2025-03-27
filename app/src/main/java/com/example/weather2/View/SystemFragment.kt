package com.example.weather2.View

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.weather2.R
import com.example.weather2.databinding.FragmentSystemBinding

class SystemFragment : Fragment() {
    private lateinit var bindingSystem: FragmentSystemBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSystem = FragmentSystemBinding.inflate(inflater, container, false)
        return bindingSystem.root
    }
}