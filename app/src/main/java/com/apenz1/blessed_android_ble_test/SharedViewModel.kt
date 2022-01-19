package com.apenz1.blessed_android_ble_test

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
    private val _totalDistance = MutableLiveData<Int>()
    private val _movementDirection = MutableLiveData<String>()

    private val _stepMovementCommand = MutableLiveData<String>()
    private val _continuousMovementCommand = MutableLiveData<String>()
    private val _shutterCommand = MutableLiveData<Boolean>()

    //-- Public vars --//
    val preShutterWaitTime: LiveData<Int> = _preShutterWaitTime
    val postShutterWaitTime: LiveData<Int> = _postShutterWaitTime
    val shuttersPerStep: LiveData<Int> = _shuttersPerStep
    val stepSize: LiveData<Int> = _stepSize
    val returnToStartPosition: LiveData<Boolean> = _returnToStartPosition
    val operationMode: LiveData<String> = _operationMode
    val totalDistance: LiveData<Int> = _totalDistance
    val movementDirection: LiveData<String> = _movementDirection

    val stepMovementCommand: LiveData<String> = _stepMovementCommand
    val continuousMovementCommand: LiveData<String> = _continuousMovementCommand
    val shutterCommand: LiveData<Boolean> = _shutterCommand


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

    fun setStepMovementCommand(newVal: String?) {
        _stepMovementCommand.value = newVal!!
    }

    fun setContinuousMovementCommand(newVal: String?) {
        _continuousMovementCommand.value = newVal!!
    }

    fun setShutterCommand(newVal: Boolean?) {
        _shutterCommand.value = newVal!!
    }

    fun setTotalDistance(newVal: Int?) {
        _totalDistance.value = newVal!!
    }

    fun setMovementDirection(newVal: String?) {
        _movementDirection.value = newVal!!
    }
}