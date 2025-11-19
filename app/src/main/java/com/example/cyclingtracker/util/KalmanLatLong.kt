package com.example.cyclingtracker.util

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A simple Kalman filter implementation for smoothing latitude and longitude.
 * This implementation is basic and designed to reduce GPS jitter.
 *
 * @param Q_metres_per_second The process noise variance, representing how much we expect
 * the location to "jump" randomly between measurements (in meters per second).
 * A higher value trusts the new measurements more, a lower value trusts
 * the prediction more. 3f is a reasonable default.
 */
class KalmanLatLong(private val Q_metres_per_second: Float) {
    private var minAccuracy: Float = 1f
    private var timeStamp: Long = 0 // in milliseconds
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    // P matrix is a 2x2 matrix for variance.
    // [ P[0][0] P[0][1] ]
    // [ P[1][0] P[1][1] ]
    private var variance: Float = -1.0f // P, square of uncertainty.

    /**
     * Processes a new location update.
     *
     * @param lat Latitude of the new measurement.
     * @param lon Longitude of the new measurement.
     * @param accuracy Accuracy of the new measurement (in meters).
     * @param newTimeStamp Timestamp of the new measurement (in milliseconds).
     * @return A SmoothedLocation object containing the filtered latitude and longitude.
     */
    fun process(lat: Double, lon: Double, accuracy: Float, newTimeStamp: Long): SmoothedLocation {
        var accuracy = accuracy
        if (accuracy < minAccuracy) {
            accuracy = minAccuracy
        }

        if (variance < 0) {
            // First measurement, initialize the filter.
            this.timeStamp = newTimeStamp
            this.latitude = lat
            this.longitude = lon
            this.variance = accuracy * accuracy
        } else {
            // Kalman filter logic
            val timeDelta = newTimeStamp - this.timeStamp
            if (timeDelta > 0) {
                // Predict state
                variance += timeDelta * Q_metres_per_second * Q_metres_per_second / 1000.0f
                this.timeStamp = newTimeStamp
            }

            // Calculate Kalman gain
            val K = variance / (variance + accuracy * accuracy)
            // Update state with measurement
            this.latitude += K * (lat - this.latitude)
            this.longitude += K * (lon - this.longitude)
            // Update variance
            this.variance = (1 - K) * this.variance
        }

        return SmoothedLocation(this.latitude, this.longitude)
    }

    data class SmoothedLocation(val latitude: Double, val longitude: Double)
}