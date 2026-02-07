package com.mrseong.picks.domain.place.service

import com.mrseong.picks.api.dto.PlaceCreateRequest
import com.mrseong.picks.api.dto.PlaceUpdateRequest
import com.mrseong.picks.common.exception.DuplicateException
import com.mrseong.picks.common.exception.NotFoundException
import com.mrseong.picks.domain.place.entity.Place
import com.mrseong.picks.domain.place.entity.PlaceType
import com.mrseong.picks.domain.place.repository.PlaceRepository
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PlaceServiceTest {

    @MockK
    private lateinit var placeRepository: PlaceRepository

    @InjectMockKs
    private lateinit var placeService: PlaceService

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Nested
    @DisplayName("getAllPlaces")
    inner class GetAllPlaces {

        @Test
        fun getAllPlaces_whenTypeIsNull_shouldReturnAllPlaces() {
            // given
            val places = listOf(
                createPlace(1L, "맛집1", PlaceType.RESTAURANT),
                createPlace(2L, "놀이터1", PlaceType.KIDS_PLAYGROUND)
            )
            every { placeRepository.findAll() } returns places

            // when
            val result = placeService.getAllPlaces(null)

            // then
            assertEquals(2, result.size)
            verify { placeRepository.findAll() }
        }

        @Test
        fun getAllPlaces_whenTypeIsSpecified_shouldReturnFilteredPlaces() {
            // given
            val restaurants = listOf(
                createPlace(1L, "맛집1", PlaceType.RESTAURANT),
                createPlace(2L, "맛집2", PlaceType.RESTAURANT)
            )
            every { placeRepository.findByType(PlaceType.RESTAURANT) } returns restaurants

            // when
            val result = placeService.getAllPlaces(PlaceType.RESTAURANT)

            // then
            assertEquals(2, result.size)
            result.forEach { assertEquals(PlaceType.RESTAURANT, it.type) }
            verify { placeRepository.findByType(PlaceType.RESTAURANT) }
        }
    }

    @Nested
    @DisplayName("getPlace")
    inner class GetPlace {

        @Test
        fun getPlace_whenIdExists_shouldReturnPlace() {
            // given
            val place = createPlace(1L, "맛집1", PlaceType.RESTAURANT)
            every { placeRepository.findById(1L) } returns Optional.of(place)

            // when
            val result = placeService.getPlace(1L)

            // then
            assertEquals("맛집1", result.name)
            assertEquals(PlaceType.RESTAURANT, result.type)
        }

        @Test
        fun getPlace_whenIdNotExists_shouldThrowNotFoundException() {
            // given
            every { placeRepository.findById(999L) } returns Optional.empty()

            // when & then
            assertThrows<NotFoundException> {
                placeService.getPlace(999L)
            }
        }
    }

    @Nested
    @DisplayName("createPlace")
    inner class CreatePlace {

        @Test
        fun createPlace_whenValidRequest_shouldReturnCreatedPlace() {
            // given
            val request = PlaceCreateRequest(
                name = "새 맛집",
                type = PlaceType.RESTAURANT,
                address = "서울시 강남구",
                latitude = 37.5,
                longitude = 127.0,
                description = "맛있는 집",
                grade = 1
            )
            val savedPlace = createPlace(1L, "새 맛집", PlaceType.RESTAURANT)

            every { placeRepository.existsByNameAndAddress("새 맛집", "서울시 강남구") } returns false
            every { placeRepository.save(any()) } returns savedPlace

            // when
            val result = placeService.createPlace(request)

            // then
            assertEquals("새 맛집", result.name)
            verify { placeRepository.save(any()) }
        }

        @Test
        fun createPlace_whenDuplicateNameAndAddress_shouldThrowDuplicateException() {
            // given
            val request = PlaceCreateRequest(
                name = "기존 맛집",
                type = PlaceType.RESTAURANT,
                address = "서울시 강남구",
                latitude = 37.5,
                longitude = 127.0
            )
            every { placeRepository.existsByNameAndAddress("기존 맛집", "서울시 강남구") } returns true

            // when & then
            assertThrows<DuplicateException> {
                placeService.createPlace(request)
            }
            verify(exactly = 0) { placeRepository.save(any()) }
        }

        @Test
        fun createPlace_whenSameAddressDifferentName_shouldSucceed() {
            // given
            val request = PlaceCreateRequest(
                name = "다른 맛집",
                type = PlaceType.RESTAURANT,
                address = "서울시 강남구",
                latitude = 37.5,
                longitude = 127.0
            )
            val savedPlace = createPlace(1L, "다른 맛집", PlaceType.RESTAURANT)

            every { placeRepository.existsByNameAndAddress("다른 맛집", "서울시 강남구") } returns false
            every { placeRepository.save(any()) } returns savedPlace

            // when
            val result = placeService.createPlace(request)

            // then
            assertNotNull(result)
            verify { placeRepository.save(any()) }
        }
    }

    @Nested
    @DisplayName("updatePlace")
    inner class UpdatePlace {

        @Test
        fun updatePlace_whenIdExists_shouldReturnUpdatedPlace() {
            // given
            val existingPlace = createPlace(1L, "기존 맛집", PlaceType.RESTAURANT)
            val request = PlaceUpdateRequest(
                name = "수정된 맛집",
                grade = 2
            )

            every { placeRepository.findById(1L) } returns Optional.of(existingPlace)
            every { placeRepository.save(any()) } answers { firstArg() }

            // when
            val result = placeService.updatePlace(1L, request)

            // then
            assertEquals("수정된 맛집", result.name)
            verify { placeRepository.save(any()) }
        }

        @Test
        fun updatePlace_whenIdNotExists_shouldThrowNotFoundException() {
            // given
            val request = PlaceUpdateRequest(name = "수정")
            every { placeRepository.findById(999L) } returns Optional.empty()

            // when & then
            assertThrows<NotFoundException> {
                placeService.updatePlace(999L, request)
            }
        }

        @Test
        fun updatePlace_whenPartialUpdate_shouldKeepNullFieldsUnchanged() {
            // given
            val existingPlace = createPlace(1L, "맛집", PlaceType.RESTAURANT).apply {
                description = "기존 설명"
                grade = 1
            }
            val request = PlaceUpdateRequest(name = "새 이름")  // description, grade는 null

            every { placeRepository.findById(1L) } returns Optional.of(existingPlace)
            every { placeRepository.save(any()) } answers { firstArg() }

            // when
            val result = placeService.updatePlace(1L, request)

            // then
            assertEquals("새 이름", result.name)
            assertEquals("기존 설명", existingPlace.description)
            assertEquals(1, existingPlace.grade)
        }
    }

    @Nested
    @DisplayName("deletePlace")
    inner class DeletePlace {

        @Test
        fun deletePlace_whenIdExists_shouldSoftDelete() {
            // given
            val place = createPlace(1L, "삭제할 맛집", PlaceType.RESTAURANT)
            every { placeRepository.findById(1L) } returns Optional.of(place)
            every { placeRepository.delete(place) } just runs

            // when
            placeService.deletePlace(1L)

            // then
            verify { placeRepository.delete(place) }
        }

        @Test
        fun deletePlace_whenIdNotExists_shouldThrowNotFoundException() {
            // given
            every { placeRepository.findById(999L) } returns Optional.empty()

            // when & then
            assertThrows<NotFoundException> {
                placeService.deletePlace(999L)
            }
        }
    }

    @Nested
    @DisplayName("getMarkers")
    inner class GetMarkers {

        @Test
        fun getMarkers_whenNoFilters_shouldReturnAllMarkers() {
            // given
            val places = listOf(
                createPlace(1L, "맛집1", PlaceType.RESTAURANT),
                createPlace(2L, "놀이터1", PlaceType.KIDS_PLAYGROUND)
            )
            every { placeRepository.findAll() } returns places

            // when
            val result = placeService.getMarkers(null, null, null, null, null)

            // then
            assertEquals(2, result.size)
        }

        @Test
        fun getMarkers_whenTypeSpecified_shouldReturnFilteredMarkers() {
            // given
            val restaurants = listOf(createPlace(1L, "맛집1", PlaceType.RESTAURANT))
            every { placeRepository.findByType(PlaceType.RESTAURANT) } returns restaurants

            // when
            val result = placeService.getMarkers(PlaceType.RESTAURANT, null, null, null, null)

            // then
            assertEquals(1, result.size)
        }

        @Test
        fun getMarkers_whenBoundsSpecified_shouldReturnMarkersWithinBounds() {
            // given
            val places = listOf(createPlace(1L, "맛집1", PlaceType.RESTAURANT))
            every { placeRepository.findWithinBounds(37.0, 126.0, 38.0, 128.0) } returns places

            // when
            val result = placeService.getMarkers(null, 37.0, 126.0, 38.0, 128.0)

            // then
            assertEquals(1, result.size)
            verify { placeRepository.findWithinBounds(37.0, 126.0, 38.0, 128.0) }
        }

        @Test
        fun getMarkers_whenTypeAndBoundsSpecified_shouldApplyBothFilters() {
            // given
            val restaurants = listOf(createPlace(1L, "맛집1", PlaceType.RESTAURANT))
            every {
                placeRepository.findByTypeWithinBounds(PlaceType.RESTAURANT, 37.0, 126.0, 38.0, 128.0)
            } returns restaurants

            // when
            val result = placeService.getMarkers(PlaceType.RESTAURANT, 37.0, 126.0, 38.0, 128.0)

            // then
            assertEquals(1, result.size)
            verify { placeRepository.findByTypeWithinBounds(PlaceType.RESTAURANT, 37.0, 126.0, 38.0, 128.0) }
        }
    }

    private fun createPlace(id: Long, name: String, type: PlaceType): Place {
        return Place(
            id = id,
            name = name,
            type = type,
            address = "서울시 테스트구",
            latitude = 37.5,
            longitude = 127.0,
            grade = 1
        )
    }
}
