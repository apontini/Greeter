package me.apontini.autogreeter.annotations

/**
 * Format is MM-dd and HH:mm
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class Greeter(val monthDay: String, val hourMinute: String)