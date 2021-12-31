package me.apontini

import kotlinx.coroutines.runBlocking
import scheduleGreetings


fun main() {
    runBlocking {
        scheduleGreetings()
    }
}