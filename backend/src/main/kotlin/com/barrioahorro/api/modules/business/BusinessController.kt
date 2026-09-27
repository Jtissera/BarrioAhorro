package com.barrioahorro.api.modules.business

import com.barrioahorro.api.modules.business.dto.BusinessResponse
import com.barrioahorro.api.modules.business.dto.ScheduleSlotResponse
import com.barrioahorro.api.modules.business.dto.UpdateBusinessRequest
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@RestController
@RequestMapping("/api/business")
class BusinessController(
    private val businessRepository: BusinessRepository,
    private val categoryRepository: CategoryRepository,
    private val scheduleRepository: BusinessScheduleRepository,
) {

    @Transactional
    @GetMapping("/me")
    fun getMyBusiness(authentication: Authentication): ResponseEntity<BusinessResponse> {
        val userId = authentication.principal as Long
        val business = businessRepository.findById(userId)
            .orElseThrow { notFound() }

        return ResponseEntity.ok(business.toResponse())
    }

    @Transactional
    @PatchMapping("/me")
    fun updateMyBusiness(
        authentication: Authentication,
        @RequestBody request: UpdateBusinessRequest,
    ): ResponseEntity<BusinessResponse> {
        val userId = authentication.principal as Long
        val business = businessRepository.findById(userId)
            .orElseThrow { notFound() }

        request.businessName?.let { business.nombreNegocio = it.trim() }
        request.categoryId?.let { categoryId ->
            val category = categoryRepository.findById(categoryId.toShort())
                .orElseThrow {
                    org.springframework.web.server.ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Category $categoryId does not exist",
                    )
                }
            business.rubro = category
        }
        request.direccion?.let { business.direccion = it.trim() }
        request.descripcion?.let { business.descripcion = it.trim() }

        if (request.latitud != null && request.longitud != null) {
            business.ubicacion = geometryFactory.createPoint(
                Coordinate(request.longitud, request.latitud), // ojo: x=lng, y=lat
            )
        }

        val saved = businessRepository.save(business)

        request.horarios?.let { slots ->
            scheduleRepository.deleteByComercioId(userId)
            val entities = slots.map {
                BusinessScheduleEntity(
                    comercioId = userId,
                    diaSemana = it.diaSemana,
                    horaInicio = LocalTime.parse(it.horaInicio, timeFormatter),
                    horaFin = LocalTime.parse(it.horaFin, timeFormatter),
                    activo = it.activo,
                )
            }
            scheduleRepository.saveAll(entities)
        }

        return ResponseEntity.ok(saved.toResponse())
    }

    private fun notFound() =
        org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Business profile not found")

    private fun BusinessEntity.toResponse(): BusinessResponse {
        val horarios = scheduleRepository.findByComercioIdOrderByDiaSemanaAscHoraInicioAsc(usuarioId)
            .map {
                ScheduleSlotResponse(
                    diaSemana = it.diaSemana,
                    horaInicio = it.horaInicio.format(timeFormatter),
                    horaFin = it.horaFin.format(timeFormatter),
                    activo = it.activo,
                )
            }

        return BusinessResponse(
            userId = usuarioId,
            businessName = nombreNegocio,
            categoryId = rubro?.id as Int?,
            categoryName = rubro?.nombre,
            direccion = direccion,
            latitud = ubicacion?.y,
            longitud = ubicacion?.x,
            descripcion = descripcion,
            horarios = horarios,
            onboardingCompleted = onboardingCompletado,
        )
    }
}