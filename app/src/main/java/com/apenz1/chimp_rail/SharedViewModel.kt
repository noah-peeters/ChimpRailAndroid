package com.apenz1.chimp_rail

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

// Class used for sharing user input data between activity and fragments
class SharedViewModel : ViewModel() {
    //-- Private vars --//
    private val _preShutterWaitTime = MutableLiveData<Int>()
    private val _postShutterWaitTime = MutableLiveData<Int>()
    private val _shuttersPerStep = MutableLiveData<Int>()
    private val _stepSize = MutableLiveData<Int>()
    private val _returnToStartPosition = MutableLiveData<Boolean>()
    private val _operationMode = MutableLiveData<String>()
    private val _totalStackingDistance = MutableLiveData<Int>()
    private val _movementDirection = MutableLiveData<String>()
    private val _startPositionStacking = MutableLiveData<Int>()
    private val _endPositionStacking = MutableLiveData<Int>()
    private val _currentMotorPosition = MutableLiveData<Int>()
    private val _stepMovementCommand = MutableLiveData<String>()
    private val _continuousMovementCommand = MutableLiveData<String>()
    private val _shutterCommand = MutableLiveData<Boolean>()
    private val _stopStackingCommand = MutableLiveData<Boolean>()
    private val _stackingProgressRawText = MutableLiveData<String>()

    //-- Public vars --//
    val preShutterWaitTime: LiveData<Int> = _preShutterWaitTime
    val postShutterWaitTime: LiveData<Int> = _postShutterWaitTime
    val shuttersPerStep: LiveData<Int> = _shuttersPerStep
    val stepSize: LiveData<Int> = _stepSize
    val returnToStartPosition: LiveData<Boolean> = _returnToStartPosition
    val operationMode: LiveData<String> = _operationMode
    val totalStackingDistance: LiveData<Int> = _totalStackingDistance
    val movementDirection: LiveData<String> = _movementDirection
    val startPositionStacking: LiveData<Int> = _startPositionStacking
    val endPositionStacking: LiveData<Int> = _endPositionStacking
    val currentMotorPosition: LiveData<Int> = _currentMotorPosition
    val stepMovementCommand: LiveData<String> = _stepMovementCommand
    val continuousMovementCommand: LiveData<String> = _continuousMovementCommand
    val shutterCommand: LiveData<Boolean> = _shutterCommand
    val stopStackingCommand: LiveData<Boolean> = _stopStackingCommand
    val stackingProgressRawText: LiveData<String> = _stackingProgressRawText


    //-- Setter functions --//
    fun setStepSize(newVal: Int?) {
        _stepSize.value = newVal!!
    }

    fun setPreShutterWaitTime(newVal: Int?) {
        _preShutterWaitTime.value = newVal!!
    }

    fun setPostShutterWaitTime(newVal: Int?) {
        _postShutterWaitTime.value = newVal!!
    }

    fun setShuttersPerStep(newVal: Int?) {
        _shuttersPerStep.value = newVal!!
    }

    fun setReturnToStartPosition(newVal: Boolean?) {
        _returnToStartPosition.value = newVal!!
    }

    fun setOperationMode(newVal: String?) {
        _operationMode.value = newVal!!
    }

    fun setStartPositionStacking(newVal: Int?) {
        _startPositionStacking.value = newVal!!
    }

    fun setEndPositionStacking(newVal: Int?) {
        _endPositionStacking.value = newVal!!
    }

    fun setCurrentMotorPosition(newVal: Int?) {
        _currentMotorPosition.value = newVal!!
    }

    fun setStepMovementCommand(newVal: String?) {
        _stepMovementCommand.value = newVal!!
    }

    fun setContinuousMovementCommand(newVal: String?) {
        _continuousMovementCommand.value = newVal!!
    }

    fun setShutterCommand(newVal: Boolean?) {
        _shutterCommand.value = newVal!!
    }

    fun setStopStackingCommand(newVal: Boolean?) {
        _stopStackingCommand.value = newVal!!
    }

    fun setTotalStackingDistance(newVal: Int?) {
        _totalStackingDistance.value = newVal!!
    }

    fun setMovementDirection(newVal: String?) {
        _movementDirection.value = newVal!!
    }

    fun setStackingProgressRawText(newVal: String?) {
        _stackingProgressRawText.value = newVal!!
    }
}