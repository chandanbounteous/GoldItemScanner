package com.kanishk.golditemscanner.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.kanishk.golditemscanner.databinding.FragmentGoldRateBinding
import kotlinx.coroutines.launch

class GoldRateFragment : Fragment() {

    private var _binding: FragmentGoldRateBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    val goldRateViewModel = lazy {
        ViewModelProvider(this).get(GoldRateViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
//        val homeViewModel =
//            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentGoldRateBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val goldRateText: TextView = binding.goldRate
        goldRateViewModel.value.goldRate.observe(viewLifecycleOwner) {
            goldRateText.text = it
        }
        val goldRateDateText: TextView = binding.goldRateDate
        goldRateViewModel.value.goldRateDate.observe(viewLifecycleOwner) {
            goldRateDateText.text = it
        }

        return root
    }

    override fun onResume() {
        super.onResume()
        // Launch a coroutine to call the suspend function
        viewLifecycleOwner.lifecycleScope.launch {
            goldRateViewModel.value.loadGoldRate(requireActivity().application.baseContext)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}