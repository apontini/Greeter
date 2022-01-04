package me.apontini.autogreeter.annotations.processors

import com.squareup.kotlinpoet.*
import java.time.LocalDateTime

class FileSpecFactory {
    fun create(funcCallList: List<String>): FileSpec {
        val funcBuilder = FunSpec.builder("scheduleGreetings")
            .addModifiers(KModifier.SUSPEND)
            .beginControlFlow("coroutineScope {")
        funcCallList.forEach { statement -> funcBuilder.addStatement(statement) }
        funcBuilder.endControlFlow()

        //File generation
        return FileSpec.builder("", "ScheduledGreetings")
            .addImport("kotlinx.coroutines", "async", "coroutineScope", "delay")
            .addImport("java.time", "LocalDateTime", "format.DateTimeFormatterBuilder")
            .addImport("java.time.temporal", "ChronoField", "ChronoUnit")
            .addFunction(funcBuilder.build())
            .addFunction( //utility functions
                FunSpec.builder("scheduleGreet")
                    .addModifiers(KModifier.SUSPEND, KModifier.PRIVATE)
                    .addParameter("monthDay", String::class)
                    .addParameter("hourMinute", String::class)
                    .addParameter("greetFunc", LambdaTypeName.get(returnType = Unit::class.asTypeName()))
                    .beginControlFlow("while(true) {")
                    .addStatement(
                        """
                         val now = LocalDateTime.now()
                         val parsedDate = (monthDay + " | " + hourMinute) parseToYear now.year andRescheduleIfBefore now
                         delay(now until parsedDate)
                         greetFunc()
                        """.trimIndent()
                    )
                    .endControlFlow()
                    .build()
            )
            .addFunction(
                FunSpec.builder("until")
                    .receiver(LocalDateTime::class)
                    .addModifiers(KModifier.PRIVATE, KModifier.INFIX)
                    .addParameter("untilDate", LocalDateTime::class)
                    .addStatement("return this.until(untilDate, ChronoUnit.MILLIS)")
                    .returns(Long::class)
                    .build()
            )
            .addFunction(
                FunSpec.builder("parseToYear")
                    .receiver(String::class)
                    .addModifiers(KModifier.PRIVATE, KModifier.INFIX)
                    .addParameter("now", Int::class)
                    .addStatement(
                        """
                            return LocalDateTime.parse(
                                this,
                                DateTimeFormatterBuilder()
                                    .appendPattern("MM-dd | HH:mm")
                                    .parseDefaulting(ChronoField.YEAR, now.toLong())
                                    .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                                    .toFormatter()
                            )
                        """.trimIndent()
                    )
                    .returns(LocalDateTime::class)
                    .build()
            )
            .addFunction(
                FunSpec.builder("andRescheduleIfBefore")
                    .receiver(LocalDateTime::class)
                    .addModifiers(KModifier.PRIVATE, KModifier.INFIX)
                    .addParameter("now", LocalDateTime::class)
                    .beginControlFlow("return when {")
                    .beginControlFlow("this.isBefore(now) -> ")
                    .addStatement("this.plusYears(1)")
                    .endControlFlow()
                    .beginControlFlow("else -> ")
                    .addStatement("this")
                    .endControlFlow()
                    .endControlFlow()
                    .returns(LocalDateTime::class)
                    .build()
            )
            .build()
    }
}