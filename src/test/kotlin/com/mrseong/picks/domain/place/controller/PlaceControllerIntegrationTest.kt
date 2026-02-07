package com.mrseong.picks.domain.place.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.mrseong.picks.api.dto.PlaceCreateRequest
import com.mrseong.picks.api.dto.PlaceUpdateRequest
import com.mrseong.picks.domain.auth.controller.LoginRequest
import com.mrseong.picks.domain.place.entity.Place
import com.mrseong.picks.domain.place.entity.PlaceType
import com.mrseong.picks.domain.place.repository.PlaceRepository
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlaceControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var placeRepository: PlaceRepository

    private lateinit var authToken: String

    @BeforeAll
    fun setUpAuth() {
        val loginRequest = LoginRequest("test-admin-password")
        val result = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andReturn()

        val response = objectMapper.readTree(result.response.contentAsString)
        authToken = response.get("data").get("token").asText()
    }

    @BeforeEach
    fun setUp() {
        placeRepository.deleteAll()
    }

    @Nested
    @DisplayName("GET /api/places")
    inner class GetAllPlaces {

        @Test
        fun getAllPlaces_whenPlacesExist_shouldReturnAllPlaces() {
            // given
            createTestPlace("맛집1", PlaceType.RESTAURANT)
            createTestPlace("놀이터1", PlaceType.KIDS_PLAYGROUND)

            // when & then
            mockMvc.perform(get("/api/places"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
        }

        @Test
        fun getAllPlaces_whenTypeSpecified_shouldReturnFilteredPlaces() {
            // given
            createTestPlace("맛집1", PlaceType.RESTAURANT)
            createTestPlace("맛집2", PlaceType.RESTAURANT)
            createTestPlace("놀이터1", PlaceType.KIDS_PLAYGROUND)

            // when & then
            mockMvc.perform(get("/api/places").param("type", "RESTAURANT"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.length()").value(2))
        }
    }

    @Nested
    @DisplayName("GET /api/places/{id}")
    inner class GetPlace {

        @Test
        fun getPlace_whenIdExists_shouldReturnPlace() {
            // given
            val place = createTestPlace("테스트 맛집", PlaceType.RESTAURANT)

            // when & then
            mockMvc.perform(get("/api/places/${place.id}"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.name").value("테스트 맛집"))
                .andExpect(jsonPath("$.data.type").value("RESTAURANT"))
        }

        @Test
        fun getPlace_whenIdNotExists_shouldReturn404() {
            mockMvc.perform(get("/api/places/99999"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
        }
    }

    @Nested
    @DisplayName("POST /api/places")
    inner class CreatePlace {

        @Test
        fun createPlace_whenAuthenticated_shouldReturnCreatedPlace() {
            // given
            val request = PlaceCreateRequest(
                name = "새 맛집",
                type = PlaceType.RESTAURANT,
                address = "서울시 강남구",
                latitude = 37.5,
                longitude = 127.0,
                grade = 1
            )

            // when & then
            mockMvc.perform(
                post("/api/places")
                    .header("Authorization", "Bearer $authToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.data.name").value("새 맛집"))
        }

        @Test
        fun createPlace_whenNotAuthenticated_shouldReturn401() {
            val request = PlaceCreateRequest(
                name = "새 맛집",
                type = PlaceType.RESTAURANT,
                address = "서울시 강남구",
                latitude = 37.5,
                longitude = 127.0
            )

            mockMvc.perform(
                post("/api/places")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isUnauthorized)
        }

        @Test
        fun createPlace_whenDuplicateNameAndAddress_shouldReturn409() {
            // given
            createTestPlace("기존 맛집", PlaceType.RESTAURANT, "서울시 강남구")

            val request = PlaceCreateRequest(
                name = "기존 맛집",
                type = PlaceType.RESTAURANT,
                address = "서울시 강남구",
                latitude = 37.5,
                longitude = 127.0
            )

            // when & then
            mockMvc.perform(
                post("/api/places")
                    .header("Authorization", "Bearer $authToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isConflict)
        }
    }

    @Nested
    @DisplayName("PUT /api/places/{id}")
    inner class UpdatePlace {

        @Test
        fun updatePlace_whenAuthenticated_shouldReturnUpdatedPlace() {
            // given
            val place = createTestPlace("기존 맛집", PlaceType.RESTAURANT)
            val request = PlaceUpdateRequest(name = "수정된 맛집")

            // when & then
            mockMvc.perform(
                put("/api/places/${place.id}")
                    .header("Authorization", "Bearer $authToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.name").value("수정된 맛집"))
        }

        @Test
        fun updatePlace_whenNotAuthenticated_shouldReturn401() {
            val place = createTestPlace("맛집", PlaceType.RESTAURANT)
            val request = PlaceUpdateRequest(name = "수정")

            mockMvc.perform(
                put("/api/places/${place.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isUnauthorized)
        }
    }

    @Nested
    @DisplayName("DELETE /api/places/{id}")
    inner class DeletePlace {

        @Test
        fun deletePlace_whenAuthenticated_shouldSoftDeletePlace() {
            // given
            val place = createTestPlace("삭제할 맛집", PlaceType.RESTAURANT)

            // when & then
            mockMvc.perform(
                delete("/api/places/${place.id}")
                    .header("Authorization", "Bearer $authToken")
            )
                .andExpect(status().isNoContent)

            mockMvc.perform(get("/api/places/${place.id}"))
                .andExpect(status().isNotFound)
        }

        @Test
        fun deletePlace_whenNotAuthenticated_shouldReturn401() {
            val place = createTestPlace("맛집", PlaceType.RESTAURANT)

            mockMvc.perform(delete("/api/places/${place.id}"))
                .andExpect(status().isUnauthorized)
        }
    }

    private fun createTestPlace(
        name: String,
        type: PlaceType,
        address: String = "서울시 테스트구"
    ): Place {
        return placeRepository.save(
            Place(
                name = name,
                type = type,
                address = address,
                latitude = 37.5,
                longitude = 127.0,
                grade = 1
            )
        )
    }
}
