package com.mrseong.picks.batch

import com.mrseong.picks.domain.place.repository.PlaceRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.net.URLEncoder

@Component
@Profile("batch")
@ConditionalOnProperty(name = ["batch.job"], havingValue = "sync-google")
class SyncGooglePlacesRunner(
    private val placeRepository: PlaceRepository,
    @Value("\${batch.google-api-key}") private val googleApiKey: String
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)
    private val restTemplate = RestTemplate()

    override fun run(args: ApplicationArguments) {
        if (googleApiKey.isBlank()) {
            log.error("GOOGLE_API_KEY 환경변수가 설정되지 않았습니다.")
            return
        }

        val limitStr = args.getOptionValues("limit")?.firstOrNull() ?: "100"
        val limit = (limitStr.toIntOrNull() ?: 100).coerceIn(1, 500)

        log.info("Google Places 동기화 시작")
        log.info("Google API Key: ${googleApiKey.take(10)}...")
        log.info("처리 제한: ${limit}개")

        val allPlaces = placeRepository.findByGoogleRatingIsNull()
        val places = allPlaces.take(limit)
        log.info("총 ${allPlaces.size}개 미동기화 장소 중 ${places.size}개 처리 예정")

        if (places.isEmpty()) {
            log.info("모든 장소가 이미 동기화되어 있습니다.")
            return
        }

        var success = 0
        var notFound = 0
        var failed = 0

        for (place in places) {
            log.info("[${place.id}] ${place.name} 처리 중...")

            try {
                val googleData = searchGooglePlace(place.name, place.address, place.latitude, place.longitude)

                if (googleData != null) {
                    place.googlePlaceId = googleData.placeId
                    place.googleRating = googleData.rating
                    place.googleRatingsTotal = googleData.ratingsTotal
                    placeRepository.save(place)
                    log.info("  성공: ${googleData.rating ?: "N/A"}점 (${googleData.ratingsTotal ?: 0}개 리뷰)")
                    success++
                } else {
                    log.warn("  Google에서 찾을 수 없음: ${place.name}")
                    notFound++
                }

                Thread.sleep(200)
            } catch (e: Exception) {
                log.error("  에러: ${e.message}")
                failed++
            }
        }

        log.info("========== 결과 ==========")
        log.info("성공: ${success}개")
        log.info("미발견: ${notFound}개")
        log.info("실패: ${failed}개")
        log.info("==========================")
    }

    @Suppress("UNCHECKED_CAST")
    private fun searchGooglePlace(name: String, address: String, lat: Double, lng: Double): GooglePlaceData? {
        return try {
            val input = URLEncoder.encode("$name $address", "UTF-8")
            val url = "https://maps.googleapis.com/maps/api/place/findplacefromtext/json" +
                "?input=$input" +
                "&inputtype=textquery" +
                "&locationbias=point:$lat,$lng" +
                "&fields=place_id,rating,user_ratings_total,name" +
                "&key=$googleApiKey"

            val response = restTemplate.getForObject(url, Map::class.java)
            val status = response?.get("status") as? String
            val candidates = response?.get("candidates") as? List<Map<String, Any>>

            if (status == "OK" && !candidates.isNullOrEmpty()) {
                val candidate = candidates[0]
                GooglePlaceData(
                    placeId = candidate["place_id"] as? String,
                    rating = (candidate["rating"] as? Number)?.toDouble(),
                    ratingsTotal = (candidate["user_ratings_total"] as? Number)?.toInt()
                )
            } else null
        } catch (e: Exception) {
            log.debug("Google Places 검색 실패: ${e.message}")
            null
        }
    }

    private data class GooglePlaceData(
        val placeId: String?,
        val rating: Double?,
        val ratingsTotal: Int?
    )
}
