package me.apontini.autogreeter.annotations.processors

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import me.apontini.autogreeter.annotations.Greeter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.annotation.processing.*
import javax.lang.model.element.TypeElement
import javax.lang.model.type.ExecutableType
import javax.tools.Diagnostic
import kotlin.io.path.Path

@AutoService(Processor::class)
@SupportedAnnotationTypes("me.apontini.autogreeter.annotations.Greeter")
@SupportedOptions(GreeterProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME)
class GreeterProcessor : AbstractProcessor() {
    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?): Boolean {
        roundEnv?.getElementsAnnotatedWith(Greeter::class.java)?.map { element ->
            if ((element.asType() as ExecutableType).parameterTypes.size >= 1) {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Can't process functions with arguments")
            }
            val monthDay = element.getAnnotation(Greeter::class.java).monthDay.takeIf { it isFormattedLike "MM-dd" }
                ?: processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Wrong format for month and day")
            val hourMinute = element.getAnnotation(Greeter::class.java).hourMinute.takeIf { it isFormattedLike "HH:mm" }
                ?: processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Wrong format for hours and minutes")

            """
            async { scheduleGreet("$monthDay","$hourMinute") { ${element.enclosingElement}.${element.simpleName}() } }
            """.trimIndent()
        }?.takeIf { it.isNotEmpty() }?.let {
            val funcBuilder = FunSpec.builder("scheduleGreetings")
                .addModifiers(KModifier.SUSPEND)
                .beginControlFlow("coroutineScope {")
            it.forEach { statement -> funcBuilder.addStatement(statement) }
            funcBuilder.endControlFlow()

            //File generation
            FileSpec.builder("", "ScheduledGreetings")
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
                        .addStatement("""
                            return LocalDateTime.parse(
                                this,
                                DateTimeFormatterBuilder()
                                    .appendPattern("MM-dd | HH:mm")
                                    .parseDefaulting(ChronoField.YEAR, now.toLong())
                                    .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                                    .toFormatter()
                            )
                        """.trimIndent())
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
                .writeTo(Path(processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME].orEmpty()))
        }
        return true
    }

    private infix fun String.isFormattedLike(pattern : String) = try {
        DateTimeFormatter.ofPattern(pattern).parse(this)
        true
    } catch (e : DateTimeParseException) {
        false
    }
}