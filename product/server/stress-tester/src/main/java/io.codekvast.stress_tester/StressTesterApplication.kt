package io.codekvast.stress_tester

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@ComponentScan(basePackages = ["io.codekvast"])
@EnableScheduling
class StressTesterApplication

fun main(args: Array<String>) {
    runApplication<StressTesterApplication>(*args)
}
