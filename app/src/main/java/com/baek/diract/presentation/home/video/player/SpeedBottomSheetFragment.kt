package com.baek.diract.presentation.home.video.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.setFragmentResult
import com.baek.diract.R
import com.baek.diract.databinding.FragmentSpeedBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlin.math.roundToInt

class SpeedBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentSpeedBottomSheetBinding? = null
    private val binding get() = _binding!!

    private var currentSpeed = DEFAULT_SPEED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentSpeed = arguments?.getFloat(ARG_SPEED, DEFAULT_SPEED) ?: DEFAULT_SPEED
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSpeedBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // BottomSheet 컨테이너 배경을 투명하게 설정하여 커스텀 배경이 보이도록 처리
        (dialog as? BottomSheetDialog)?.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        )?.setBackgroundResource(android.R.color.transparent)

        setupSlider()
        setupPresetButtons()
        setupStepButtons()
        updateSpeedText()
    }

    private fun setupSlider() {
        binding.speedSlider.value = currentSpeed.coerceIn(SPEED_MIN, SPEED_MAX)
        binding.speedSlider.addOnChangeListener { _, value, _ ->
            currentSpeed = value
            updateSpeedText()
        }
    }

    private fun setupPresetButtons() {
        binding.btn03.setOnClickListener { setSpeed(0.3f) }
        binding.btn05.setOnClickListener { setSpeed(0.5f) }
        binding.btn075.setOnClickListener { setSpeed(0.75f) }
        binding.btn08.setOnClickListener { setSpeed(0.8f) }
        binding.btn10.setOnClickListener { setSpeed(1.0f) }
    }

    private fun setupStepButtons() {
        binding.speedDownBtn.setOnClickListener {
            val newSpeed = (currentSpeed - SPEED_STEP).coerceAtLeast(SPEED_MIN)
            setSpeed(newSpeed)
        }
        binding.speedUpBtn.setOnClickListener {
            val newSpeed = (currentSpeed + SPEED_STEP).coerceAtMost(SPEED_MAX)
            setSpeed(newSpeed)
        }
    }

    private fun setSpeed(speed: Float) {
        // stepSize(0.05) 단위로 반올림
        currentSpeed = ((speed / SPEED_STEP).roundToInt() * SPEED_STEP)
            .coerceIn(SPEED_MIN, SPEED_MAX)
        binding.speedSlider.value = currentSpeed
        updateSpeedText()
    }

    private fun updateSpeedText() {
        binding.speedTxt.text = getString(R.string.speed_value, currentSpeed)
    }

    override fun onStart() {
        super.onStart()

        val bottomSheet = (dialog as? BottomSheetDialog)
            ?.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            ?: return

        // 최대 너비 제한
        bottomSheet.layoutParams = bottomSheet.layoutParams.apply {
            width = resources.getDimensionPixelSize(R.dimen.speed_dialog_max_width)
        }

        BottomSheetBehavior.from(bottomSheet).state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onDestroyView() {
        // dismiss 시 선택된 속도를 결과로 전달
        setFragmentResult(REQUEST_KEY, bundleOf(RESULT_SPEED to currentSpeed))
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val REQUEST_KEY = "speed_request"
        const val RESULT_SPEED = "result_speed"
        const val TAG = "SpeedBottomSheet"

        private const val ARG_SPEED = "arg_speed"
        private const val DEFAULT_SPEED = 1.0f
        private const val SPEED_MIN = 0.25f
        private const val SPEED_MAX = 1.0f
        private const val SPEED_STEP = 0.05f

        fun newInstance(currentSpeed: Float) = SpeedBottomSheetFragment().apply {
            arguments = bundleOf(ARG_SPEED to currentSpeed)
        }
    }
}
