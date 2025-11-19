package com.example.cyclingtracker.util

object CalorieCalculator {

    /**
     * Estimates the Metabolic Equivalent of Task (MET) value based on cycling speed.
     * Values are based on the Compendium of Physical Activities.
     * @param speedKmh The average cycling speed in km/h.
     * @return The estimated MET value.
     */
    fun getMetValue(speedKmh: Float): Double {
        return when {
            speedKmh < 16 -> 4.0   // Leisurely, light effort (< 10 mph)
            speedKmh < 19 -> 6.8   // Moderate effort (10-11.9 mph)
            speedKmh < 22.4 -> 8.0 // Vigorous effort (12-13.9 mph)
            speedKmh < 25.5 -> 10.0 // Very vigorous effort (14-15.9 mph)
            else -> 12.0          // Racing, > 16 mph
        }
    }

    /**
     * Calculates the total calories burned.
     * Formula: Calories Burned = (MET × Body Weight in kg × 3.5) / 200 × Duration in minutes
     *
     * @param met The Metabolic Equivalent of Task value.
     * @param weightKg The user's body weight in kilograms.
     * @param durationInSeconds The duration of the activity in seconds.
     * @return The total calories burned as a Float.
     */
    fun calculateCalories(met: Double, weightKg: Float, durationInSeconds: Long): Float {
        if (durationInSeconds == 0L) return 0f
        val durationInMinutes = durationInSeconds / 60.0
        return ((met * weightKg * 3.5) / 200.0 * durationInMinutes).toFloat()
    }
}