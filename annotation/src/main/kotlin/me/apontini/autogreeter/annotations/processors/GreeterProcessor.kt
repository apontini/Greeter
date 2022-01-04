package me.apontini.autogreeter.annotations.processors

import com.google.auto.service.AutoService
import me.apontini.autogreeter.annotations.Greeter
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
            (element.asType() as ExecutableType).parameterTypes.takeIf { it.size == 0 }
                ?: processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Can't process functions with arguments")
            val monthDay = element.getAnnotation(Greeter::class.java).monthDay.takeIf { it isFormattedLike "MM-dd" }
                ?: processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Wrong format for month and day")
            val hourMinute = element.getAnnotation(Greeter::class.java).hourMinute.takeIf { it isFormattedLike "HH:mm" }
                ?: processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Wrong format for hours and minutes")

            """
            async { scheduleGreet("$monthDay","$hourMinute") { ${element.enclosingElement}.${element.simpleName}() } }
            """.trimIndent()
        }?.takeIf { it.isNotEmpty() }?.let {
            FileSpecFactory().create(it)
                .writeTo(Path(processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME].orEmpty()))
        }
        return true
    }

    private infix fun String.isFormattedLike(pattern: String) = try {
        DateTimeFormatter.ofPattern(pattern).parse(this)
        true
    } catch (e: DateTimeParseException) {
        false
    }
}