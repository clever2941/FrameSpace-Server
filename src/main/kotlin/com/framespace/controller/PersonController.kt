package com.framespace.controller

import com.framespace.common.Result
import com.framespace.dto.PersonFilmographyDto
import com.framespace.service.PersonService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/persons")
class PersonController(
    private val personService: PersonService
) {

    @GetMapping("/{personId}/movies")
    fun getPersonMovies(
        @PathVariable personId: Int,
        @RequestParam(value = "kind", defaultValue = "cast") kind: String,
        @RequestParam(value = "sort", defaultValue = "rating") sort: String,
        @RequestParam(value = "page", defaultValue = "0") page: Long,
        @RequestParam(value = "size", defaultValue = "15") size: Long
    ): Result<PersonFilmographyDto> {
        return try {
            Result.success(personService.getFilmography(personId, kind, sort, page, size))
        } catch (e: IllegalArgumentException) {
            Result.error(404, e.message ?: "演职人员不存在")
        }
    }
}
