package com.umbrella.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Open-Meteo API 응답 DTO
 */
@Serializable
data class WeatherResponse(
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    @SerialName("timezone_abbreviation")
    val timezoneAbbreviation: String,
    val elevation: Double? = null,
    val hourly: HourlyData,
    @SerialName("hourly_units")
    val hourlyUnits: HourlyUnits? = null
)

@Serializable
data class HourlyData(
    val time: List<String>,
    @SerialName("precipitation_probability")
    val precipitationProbability: List<Int?>,
    @SerialName("temperature_2m")
    val temperature2m: List<Double?>? = null,
    @SerialName("weather_code")
    val weatherCode: List<Int?>? = null
)

@Serializable
data class HourlyUnits(
    val time: String? = null,
    @SerialName("precipitation_probability")
    val precipitationProbability: String? = null,
    @SerialName("temperature_2m")
    val temperature2m: String? = null
)
