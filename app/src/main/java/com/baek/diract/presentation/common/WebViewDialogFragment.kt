package com.baek.diract.presentation.common

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import com.baek.diract.databinding.DialogWebviewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class WebViewDialogFragment : BottomSheetDialogFragment() {

    private var _binding: DialogWebviewBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogWebviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = arguments?.getString(ARG_TITLE) ?: ""
        val url = arguments?.getString(ARG_URL) ?: ""

        binding.titleTxt.text = title
        binding.closeBtn.setOnClickListener { dismiss() }

        setupWebView()
        binding.webView.loadUrl(url)
    }

    override fun onStart() {
        super.onStart()

        val bottomSheet = (dialog as? BottomSheetDialog)
            ?.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            ?: return

        bottomSheet.layoutParams = bottomSheet.layoutParams.apply {
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }

        BottomSheetBehavior.from(bottomSheet).apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
            isDraggable = false
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
        }
        binding.webView.webViewClient = WebViewClient()
    }

    override fun onDestroyView() {
        binding.webView.destroy()
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "WebViewDialogFragment"
        private const val ARG_TITLE = "arg_title"
        private const val ARG_URL = "arg_url"

        fun newInstance(title: String, url: String) = WebViewDialogFragment().apply {
            arguments = bundleOf(ARG_TITLE to title, ARG_URL to url)
        }
    }
}
