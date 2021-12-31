# AutoGreeter

Do you find yourself struggling to remember to send seasonal greetings or to wish happy birthday to someone? Well, fear
not! AutoGreeter is here! The only thing you need to do is to create a function with no parameters and annotated
with `@Greeter` like this:

```kotlin
companion object {
    @Greeter("05-12", "00:30") //00:30 because we don't want to let people know we're a bot, right?
    fun greetSomebody() {
        //...
    }
}
```

And this method will be called on December 12 of every year at exactly 00:30.
To generate scheduled greetings just run `mvn clean install`.

Jokes aside, this is a small project that I made to study how to generate code starting from annotations using KAPT (and
not KSP since, at the time of writing, it doesn't have and expanded support). An arguably better way to handle such a
task would've been to implement some sort of Kotlin DSL such as:

```kotlin
greetings {
    greetAt("12-31", "00:00") {
        //run something
    }
}
```

Or just, you know, use some cron-like library.