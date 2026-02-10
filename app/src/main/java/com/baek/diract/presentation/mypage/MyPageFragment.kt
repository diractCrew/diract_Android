package com.baek.diract.presentation.mypage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.baek.diract.databinding.FragmentMyPageBinding

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MyPageFragment : Fragment() {
    private var _binding: FragmentMyPageBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navigationTemporaryEx()
    }
    /*
        TODO: HomeFragment에서 navigation 설정 필요
        1. nav_graph에서 수정
        2. home fragment에서
            val action = HomeFragmentDirections.actionHomeFragmentToVideoListFragment(tracksId,tracksTitle)
            findNavController().navigate(action)
     */
    private fun navigationTemporaryEx() {
        val tracksId = "AndroidTestTracks1"
        val tracksTitle = "AndTracks"
        binding.toVideoBtn.setOnClickListener {
            val action = MyPageFragmentDirections.actionMyPageFragmentToVideoNavGraph(
                tracksId,
                tracksTitle
            )
            findNavController().navigate(action)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
