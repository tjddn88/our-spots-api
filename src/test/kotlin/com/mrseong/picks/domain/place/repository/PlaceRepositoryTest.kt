package com.mrseong.picks.domain.place.repository

import com.mrseong.picks.domain.place.entity.Place
import com.mrseong.picks.domain.place.entity.PlaceType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@DataJpaTest
@ActiveProfiles("test")
class PlaceRepositoryTest {

    @Autowired
    private lateinit var placeRepository: PlaceRepository

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @BeforeEach
    fun setUp() {
        placeRepository.deleteAll()
        entityManager.flush()
        entityManager.clear()
    }

    @Nested
    @DisplayName("findByType")
    inner class FindByType {

        @Test
        fun findByType_whenTypeExists_shouldReturnFilteredPlaces() {
            // given
            createPlace("맛집1", PlaceType.RESTAURANT)
            createPlace("맛집2", PlaceType.RESTAURANT)
            createPlace("놀이터1", PlaceType.KIDS_PLAYGROUND)

            // when
            val restaurants = placeRepository.findByType(PlaceType.RESTAURANT)

            // then
            assertEquals(2, restaurants.size)
            assertTrue(restaurants.all { it.type == PlaceType.RESTAURANT })
        }
    }

    @Nested
    @DisplayName("existsByNameAndAddress")
    inner class ExistsByNameAndAddress {

        @Test
        fun existsByNameAndAddress_whenBothMatch_shouldReturnTrue() {
            // given
            createPlace("맛집", PlaceType.RESTAURANT, "서울시 강남구")

            // when & then
            assertTrue(placeRepository.existsByNameAndAddress("맛집", "서울시 강남구"))
        }

        @Test
        fun existsByNameAndAddress_whenSameAddressDifferentName_shouldReturnFalse() {
            // given
            createPlace("맛집A", PlaceType.RESTAURANT, "서울시 강남구")

            // when & then
            assertFalse(placeRepository.existsByNameAndAddress("맛집B", "서울시 강남구"))
        }

        @Test
        fun existsByNameAndAddress_whenSameNameDifferentAddress_shouldReturnFalse() {
            // given
            createPlace("맛집", PlaceType.RESTAURANT, "서울시 강남구")

            // when & then
            assertFalse(placeRepository.existsByNameAndAddress("맛집", "서울시 서초구"))
        }
    }

    @Nested
    @DisplayName("findWithinBounds")
    inner class FindWithinBounds {

        @Test
        fun findWithinBounds_whenPlacesInsideBounds_shouldReturnThem() {
            // given
            createPlace("강남 맛집", PlaceType.RESTAURANT, lat = 37.5, lng = 127.0)
            createPlace("강북 맛집", PlaceType.RESTAURANT, lat = 37.6, lng = 127.0)
            createPlace("부산 맛집", PlaceType.RESTAURANT, lat = 35.1, lng = 129.0)

            // when
            val result = placeRepository.findWithinBounds(37.4, 126.8, 37.7, 127.2)

            // then
            assertEquals(2, result.size)
        }

        @Test
        fun findWithinBounds_whenPlaceOnBoundary_shouldIncludeIt() {
            // given
            createPlace("경계 맛집", PlaceType.RESTAURANT, lat = 37.4, lng = 126.8)

            // when
            val result = placeRepository.findWithinBounds(37.4, 126.8, 37.7, 127.2)

            // then
            assertEquals(1, result.size)
        }
    }

    @Nested
    @DisplayName("findByTypeWithinBounds")
    inner class FindByTypeWithinBounds {

        @Test
        fun findByTypeWithinBounds_whenBothConditionsMatch_shouldReturnFilteredPlaces() {
            // given
            createPlace("강남 맛집", PlaceType.RESTAURANT, lat = 37.5, lng = 127.0)
            createPlace("강남 놀이터", PlaceType.KIDS_PLAYGROUND, lat = 37.5, lng = 127.0)
            createPlace("부산 맛집", PlaceType.RESTAURANT, lat = 35.1, lng = 129.0)

            // when
            val result = placeRepository.findByTypeWithinBounds(
                PlaceType.RESTAURANT, 37.4, 126.8, 37.7, 127.2
            )

            // then
            assertEquals(1, result.size)
            assertEquals("강남 맛집", result[0].name)
        }
    }

    @Nested
    @DisplayName("Soft Delete")
    inner class SoftDelete {

        @Test
        fun delete_whenCalled_shouldExcludeFromFindById() {
            // given
            val place = createPlace("삭제할 맛집", PlaceType.RESTAURANT)

            // when
            placeRepository.delete(place)
            entityManager.flush()
            entityManager.clear()

            // then
            val result = placeRepository.findById(place.id)
            assertFalse(result.isPresent)
        }

        @Test
        fun delete_whenCalled_shouldExcludeFromFindAll() {
            // given
            val place1 = createPlace("맛집1", PlaceType.RESTAURANT)
            createPlace("맛집2", PlaceType.RESTAURANT)

            // when
            placeRepository.delete(place1)
            entityManager.flush()
            entityManager.clear()

            // then
            val result = placeRepository.findAll()
            assertEquals(1, result.size)
            assertEquals("맛집2", result[0].name)
        }
    }

    private fun createPlace(
        name: String,
        type: PlaceType,
        address: String = "서울시 테스트구",
        lat: Double = 37.5,
        lng: Double = 127.0
    ): Place {
        val place = Place(
            name = name,
            type = type,
            address = address,
            latitude = lat,
            longitude = lng,
            grade = 1
        )
        entityManager.persist(place)
        entityManager.flush()
        return place
    }
}
