package com.barrioahorro.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
@ComponentScan(basePackages = ["com.barrioahorro.backend", "com.barrioahorro.api"])
@EntityScan(basePackages = ["com.barrioahorro.api"])
@EnableJpaRepositories(basePackages = ["com.barrioahorro.api"])
class BackendApplication

fun main(args: Array<String>) {
	runApplication<BackendApplication>(*args)
}