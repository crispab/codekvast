package io.codekvast.intake

import io.micronaut.runtime.Micronaut.*

fun main(args: Array<String>) {
    build()
        .args(*args)
        .packages("io.codekvast.intake")
        .start()
}

