package com.apenz1.blessed_android_ble_test

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.textfield.TextInputLayout


class SettingsScreenFragment : Fragment() {
    private lateinit var preShutterEditText: EditText
    private lateinit var stepSizeEditText: EditText
    private lateinit var shuttersPerStepEditText: EditText
    private lateinit var postShutterEditText: EditText
    private lateinit var totalDistanceEditText: EditText
    private lateinit var startToEndModeLayout: LinearLayout
    private lateinit var distanceModeLayout: LinearLayout


    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings_screen, container, false)

        preShutterEditText = view.findViewById(R.id.preShutterWaitTime_EditText)
        postShutterEditText = view.findViewById(R.id.postShutterWaitTime_EditText)
        shuttersPerStepEditText = view.findViewById(R.id.shuttersPerStep_EditText)
        stepSizeEditText = view.findViewById(R.id.stepSize_EditText)
        totalDistanceEditText = view.findViewById(R.id.distanceModeTotalDistance_EditText)
        startToEndModeLayout = view.findViewById(R.id.startToEndMode_LinearLayout)
        distanceModeLayout = view.findViewById(R.id.distanceMode_LinearLayout)

        // EditTexts setup
        setupEditText(
            preShutterEditText,
            view.findViewById(R.id.preShutterWaitTime_InputLayout),
            0,
            120
        )
        setupEditText(
            postShutterEditText,
            view.findViewById(R.id.postShutterWaitTime_InputLayout),
            0,
            120
        )
        setupEditText(
            shuttersPerStepEditText,
            view.findViewById(R.id.shuttersPerStep_InputLayout),
            1,
            10
        )
        setupEditText(
            stepSizeEditText,
            view.findViewById(R.id.stepSize_InputLayout),
            1,
            10000 // 10.000 µm = 1cm
        )
        setupEditText(
            totalDistanceEditText,
            view.findViewById(R.id.distanceModeTotalDistance_InputLayout),
            1,
            500000 // 500.000 µm = 50cm
        )

        // Checkbox setup
        sharedViewModel.setReturnToStartPosition(true)
        view.findViewById<CheckBox>(R.id.returnToStartPosition_CheckBox).setOnClickListener { v ->
            if (v is CheckBox) {
                sharedViewModel.setReturnToStartPosition(v.isChecked)
            }
        }

        //-- Operation modes setup --//
        view.findViewById<RadioButton>(R.id.startToEnd_RadioButton)
            .setOnClickListener(operationModeRadioButtonClicked)
        view.findViewById<RadioButton>(R.id.distance_RadioButton)
            .setOnClickListener(operationModeRadioButtonClicked)
        // First setup
        startToEndModeLayout.visibility = View.VISIBLE
        distanceModeLayout.visibility = View.GONE
        sharedViewModel.setOperationMode("Start to end")

        //-- Movement direction buttons setup --//
        view.findViewById<RadioButton>(R.id.forwardsMovementDirection_RadioButton)
            .setOnClickListener(movementDirectionRadioButtonClicked)
        view.findViewById<RadioButton>(R.id.backwardsMovementDirection_RadioButton)
            .setOnClickListener(movementDirectionRadioButtonClicked)
        // First setup
        sharedViewModel.setMovementDirection("FWD")

        return view
    }

    // Set correct ViewModel value (for EditTexts)
    private fun setCorrectViewModelIntData(s: Editable?, newValue: Int) {
        when {
            s === preShutterEditText.editableText -> {
                sharedViewModel.setPreShutterWaitTime(newValue)
            }
            s === postShutterEditText.editableText -> {
                sharedViewModel.setPostShutterWaitTime(newValue)
            }
            s === shuttersPerStepEditText.editableText -> {
                sharedViewModel.setShuttersPerStep(newValue)
            }
            s === stepSizeEditText.editableText -> {
                sharedViewModel.setStepSize(newValue)
            }
            s === totalDistanceEditText.editableText -> {
                sharedViewModel.setTotalDistance(newValue)
            }
        }
    }

    // Function that adds an error message on invalid number input (out of range or other problems) + range hint
    private fun setupEditText(
        editText: EditText,
        inputLayout: TextInputLayout,
        rangeMin: Int,
        rangeMax: Int
    ) {
        inputLayout.hint = "[$rangeMin-$rangeMax]"  // Set range hint

        // First set
        setCorrectViewModelIntData(editText.editableText, editText.text.toString().toInt())

        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(et: Editable?) {
                var number: Int? = null
                try {
                    // TODO: Numbers like "0006" will be converted to "6"; make error
                    number = et.toString().toInt()
                } catch (err: NumberFormatException) {
                }

                if (number != null) {
                    if (number !in rangeMin..rangeMax) {
                        // Number not in range
                        inputLayout.error = "Please enter a valid number!"
                    } else {
                        // Valid number --> remove error
                        inputLayout.error = null
                        if (editText == stepSizeEditText) {
                            setCorrectViewModelIntData(et, number)
                            Log.d("SET NUMBER", number.toString())
                            return
                        }
                    }
                } else {
                    // Invalid number (added other signs)
                    inputLayout.error = "Please enter a valid number!"
                }
                // Invalid number; set out of range
                setCorrectViewModelIntData(et, 0)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(cs: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    // Update operation mode on RadioButton click
    private val operationModeRadioButtonClicked = View.OnClickListener { view: View ->
        if (view is RadioButton) {
            val checked = view.isChecked
            var message: String? = null

            // Check which radio button was clicked
            when (view.getId()) {
                R.id.startToEnd_RadioButton ->
                    if (checked) {
                        message = "Start to end"
                        // Toggle visibility
                        startToEndModeLayout.visibility = View.VISIBLE
                        distanceModeLayout.visibility = View.GONE
                    }
                R.id.distance_RadioButton ->
                    if (checked) {
                        message = "Distance"
                        // Toggle visibility
                        startToEndModeLayout.visibility = View.GONE
                        distanceModeLayout.visibility = View.VISIBLE
                    }
            }
            if (message != null && message.isNotEmpty()) {
                // Set message
                sharedViewModel.setOperationMode(message)

                // TODO: Display snackbar notification in MainActivity
                // displaySnackbar("$message mode selected!", Snackbar.LENGTH_SHORT)
            }
        }
    }

    // Update FWD/BCK movement direction on RadioButton click
    private val movementDirectionRadioButtonClicked = View.OnClickListener { view: View ->
        if (view is RadioButton) {
            val checked = view.isChecked
            var message: String? = null

            // Check which radio button was clicked
            when (view.getId()) {
                R.id.forwardsMovementDirection_RadioButton ->
                    if (checked) {
                        message = "FWD"
                    }
                R.id.backwardsMovementDirection_RadioButton ->
                    if (checked) {
                        message = "BCK"
                    }
            }
            if (message != null && message.isNotEmpty()) {
                // Set message
                sharedViewModel.setMovementDirection(message)

                // TODO: Display snackbar notification in MainActivity
                // displaySnackbar("$message mode selected!", Snackbar.LENGTH_SHORT)
            }
        }
    }
}



