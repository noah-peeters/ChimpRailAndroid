package com.apenz1.blessed_android_ble_test

import android.animation.ObjectAnimator
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
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

        // Listen to current motor position change
        val currentPositionTextView = view.findViewById<TextView>(R.id.currentPositionTextView)
        sharedViewModel.currentMotorPosition.observe(viewLifecycleOwner) { newValue ->
            currentPositionTextView.text = newValue.toString()
        }

        setupStackingProgressLayout(view)

        // Set OnCLickListeners for step buttons. Values are updated in a ViewModel and then handled by MainActivity
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
        view.findViewById<Button>(R.id.shutterButton).setOnClickListener {
            sharedViewModel.setShutterCommand(true)
        }

        return view
    }

    // Displays stacking progress LinearLayout when needed
    private fun setupStackingProgressLayout(view: View) {
        val stackingProgressLinearLayout =
            view.findViewById<LinearLayout>(R.id.stackingProgress_LinearLayout)
        val totalDistance =
            view.findViewById<TextView>(R.id.stackingProgressTotalDistance_TextView)
        val progressBar = view.findViewById<ProgressBar>(R.id.stackingProgress_ProgressBar)
        val timeRemaining =
            view.findViewById<TextView>(R.id.stackingProgressTimeRemaining_TextView)

        stackingProgressLinearLayout.visibility = View.GONE
        totalDistance.text = "Loading..."
        timeRemaining.text = "Loading..."
        progressBar.progress = 0

        sharedViewModel.stackingProgressRawText.observe(viewLifecycleOwner) { newValue ->
            if (newValue.isNotEmpty()) {
                val valuesList = newValue.split(";")
                val stepsSinceStart = valuesList.elementAt(0).toInt()
                val totalStepsToTake = valuesList.elementAt(1).toInt()
                val stepSize = valuesList.elementAt(2).toInt()
                val timePerStep = valuesList.elementAt(3).toInt()

                val totalDistInt = totalStepsToTake * stepSize
                val timeLeftSeconds = (totalStepsToTake - stepsSinceStart) * timePerStep

                // Update progress with animation
                ObjectAnimator.ofInt(progressBar, "progress", stepsSinceStart)
                    .setDuration(800)
                    .start();
                progressBar.max = totalStepsToTake
                totalDistance.text = totalDistInt.toString()
                timeRemaining.text = DateUtils.formatElapsedTime(timeLeftSeconds.toLong())

                stackingProgressLinearLayout.visibility = View.VISIBLE
            } else {
                // Stacking has stopped; hide layout
                stackingProgressLinearLayout.visibility = View.GONE
                totalDistance.text = "Loading..."
                timeRemaining.text = "Loading..."
                progressBar.progress = 0
            }
        }

        // Stop Stacking button
        view.findViewById<Button>(R.id.stopStackingButton)?.setOnClickListener {
            sharedViewModel.setStopStackingCommand(true)
        }
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
}