package com.apenz1.blessed_android_ble_test

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels

class ControlScreenFragment : Fragment() {
    private lateinit var forwardsButton: Button
    private lateinit var backwardsButton: Button

    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_control_screen, container, false)

        // Set OnCLickListeners for step buttons to be handled by MainActivity
        val stepForwardsBtn = view.findViewById<Button>(R.id.stepForwardsButton)
        val stepBackwardsBtn = view.findViewById<Button>(R.id.stepBackwardsButton)
        stepForwardsBtn.setOnClickListener(stepMovementButtonsClickListener)
        stepBackwardsBtn.setOnClickListener(stepMovementButtonsClickListener)

        //-- OnClick- and OnTouchListeners setup --//
        // Continuous FWD/BCK movement
        forwardsButton = view.findViewById(R.id.forwardsButton)
        backwardsButton = view.findViewById(R.id.backwardsButton)
        // Step FWD/BCK movement
        forwardsButton.setOnTouchListener(onContinuousMovementButtonsTouch)
        backwardsButton.setOnTouchListener(onContinuousMovementButtonsTouch)
        // Shutter button
        view.findViewById<Button>(R.id.shutterButton).setOnClickListener(shutterButtonClickListener)

        return view
    }

    // Step movement buttons OnClickListener. Updates values in SharedModelView
    private val stepMovementButtonsClickListener = View.OnClickListener { view ->
        var directionCode: String? = null
        if (view.id == R.id.stepForwardsButton) {
            directionCode = "FWD"
        } else if (view.id == R.id.stepBackwardsButton) {
            directionCode = "BCK"
        }
        var stepSize: Int? = sharedViewModel.stepSize.value

        if (directionCode != null && stepSize != null) {
            sharedViewModel.setStepMovementCommand(directionCode + stepSize.toString())
        }
    }

    // Touch listener for continuous movement buttons. Updates values in SharedModelView
    // TODO: Doesn't call "stop" on very short click
    private val onContinuousMovementButtonsTouch =
        View.OnTouchListener { view: View, motionEvent: MotionEvent ->
            Boolean

            // Boolean to send (default is stop)
            var booleanToSend = when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    true
                }
                MotionEvent.ACTION_UP -> {
                    false
                }
                MotionEvent.ACTION_CANCEL -> {
                    false
                }
                // Don't update for other events (e.g. ACTION_MOVE,...)
                else -> {
                    return@OnTouchListener true
                }
            }

            when (view) {
                forwardsButton -> {
                    sharedViewModel.setContinuousMovementCommand("FWD$booleanToSend")
                }
                backwardsButton -> {
                    sharedViewModel.setContinuousMovementCommand("BCK$booleanToSend")
                }
            }

            // Required code
            view.performClick()
            return@OnTouchListener true
        }

    // Shutter button OnClickListener
    private val shutterButtonClickListener = View.OnClickListener { _ ->
        sharedViewModel.setShutterCommand(true)
    }
}