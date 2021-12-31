package me.apontini.greeters

import me.apontini.autogreeter.annotations.Greeter

class Holidays {
    companion object {
        @Greeter("12-31", "10:27")
        fun newYear() {
            println("123")
        }
    }
}