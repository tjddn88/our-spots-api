package com.ourspots.batch

import com.ourspots.domain.place.entity.Place
import com.ourspots.domain.place.entity.PlaceType
import com.ourspots.domain.place.repository.PlaceRepository
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.io.File

@Component
@Profile("batch")
@ConditionalOnProperty(name = ["batch.job"], havingValue = "import-places")
class ImportPlacesRunner(
    private val placeRepository: PlaceRepository,
    @Value("\${batch.kakao-rest-api-key}") private val kakaoApiKey: String
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)
    private val restTemplate = RestTemplate()

    companion object {
        private const val KAKAO_ADDRESS_URL = "https://dapi.kakao.com/v2/local/search/address.json?query={query}"
        private const val KAKAO_KEYWORD_URL = "https://dapi.kakao.com/v2/local/search/keyword.json?query={query}"
        private const val API_DELAY_MS = 200L
    }

    override fun run(args: ApplicationArguments) {
        val filePath = args.getOptionValues("file")?.firstOrNull()
        if (filePath.isNullOrBlank()) {
            log.error("--file 파라미터가 필요합니다. 예: --file=/path/to/file.xlsx")
            return
        }

        if (kakaoApiKey.isBlank()) {
            log.error("KAKAO_REST_API_KEY 환경변수가 설정되지 않았습니다.")
            return
        }

        val file = File(filePath)
        if (!file.exists()) {
            log.error("파일을 찾을 수 없습니다: $filePath")
            return
        }

        log.info("엑셀 장소 일괄 등록 시작")
        log.info("파일: $filePath")

        val places = readExcel(file)
        log.info("총 ${places.size}개 장소 발견")

        var success = 0
        var skipped = 0
        var failed = 0

        for ((index, row) in places.withIndex()) {
            val name = row["name"]?.trim() ?: ""
            val address = row["address"]?.trim() ?: ""
            val gradeStr = row["grade"]?.trim() ?: "3"
            val typeStr = row["type"]?.trim() ?: "RESTAURANT"
            val description = row["description"]?.trim()?.ifBlank { null }

            if (name.isBlank() || address.isBlank()) {
                log.warn("[SKIP] 이름 또는 주소 없음")
                skipped++
                continue
            }

            if (placeRepository.existsByNameAndAddress(name, address)) {
                log.info("[SKIP] $name - 이미 존재")
                skipped++
                continue
            }

            val grade = gradeStr.toIntOrNull() ?: 3
            val type = try {
                PlaceType.valueOf(typeStr)
            } catch (e: IllegalArgumentException) {
                PlaceType.RESTAURANT
            }

            log.info("[${index + 1}/${places.size}] $name 처리 중...")

            try {
                val coords = getCoordinates(address, name)
                if (coords == null) {
                    log.error("  좌표 찾기 실패: $name ($address)")
                    failed++
                    continue
                }

                val place = Place(
                    name = name,
                    type = type,
                    address = address,
                    latitude = coords.first,
                    longitude = coords.second,
                    description = description,
                    grade = grade
                )
                placeRepository.save(place)
                log.info("  성공 (${String.format("%.4f", coords.first)}, ${String.format("%.4f", coords.second)})")
                success++

                Thread.sleep(API_DELAY_MS)
            } catch (e: Exception) {
                log.error("  에러: ${e.message}")
                failed++
            }
        }

        log.info("========== 결과 ==========")
        log.info("성공: ${success}개")
        log.info("스킵: ${skipped}개")
        log.info("실패: ${failed}개")
        log.info("==========================")
    }

    private fun readExcel(file: File): List<Map<String, String>> {
        val workbook = WorkbookFactory.create(file)
        val sheet = workbook.getSheetAt(0)
        val rows = mutableListOf<Map<String, String>>()

        val headerRow = sheet.getRow(0) ?: return rows
        val headers = (0 until headerRow.lastCellNum).map { i ->
            headerRow.getCell(i)?.stringCellValue?.trim() ?: ""
        }

        for (i in 1..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            val map = mutableMapOf<String, String>()
            for ((j, header) in headers.withIndex()) {
                if (header.isBlank()) continue
                val cell = row.getCell(j)
                val value = when (cell?.cellType) {
                    CellType.STRING -> cell.stringCellValue
                    CellType.NUMERIC -> {
                        val num = cell.numericCellValue
                        if (num == num.toLong().toDouble()) num.toLong().toString() else num.toString()
                    }
                    CellType.BOOLEAN -> cell.booleanCellValue.toString()
                    else -> ""
                }
                map[header] = value
            }
            if (map.values.any { it.isNotBlank() }) {
                rows.add(map)
            }
        }

        workbook.close()
        return rows
    }

    private fun getCoordinates(address: String, name: String): Pair<Double, Double>? {
        return searchKakao(KAKAO_ADDRESS_URL, address)
            ?: searchKakao(KAKAO_KEYWORD_URL, "$name $address")
    }

    @Suppress("UNCHECKED_CAST")
    private fun searchKakao(url: String, query: String): Pair<Double, Double>? {
        return try {
            val headers = HttpHeaders().apply {
                set("Authorization", "KakaoAK $kakaoApiKey")
            }
            val response = restTemplate.exchange(
                url, HttpMethod.GET, HttpEntity<Void>(headers),
                Map::class.java, query
            )
            val documents = response.body?.get("documents") as? List<Map<String, Any>>
            if (!documents.isNullOrEmpty()) {
                val doc = documents[0]
                Pair((doc["y"] as String).toDouble(), (doc["x"] as String).toDouble())
            } else null
        } catch (e: Exception) {
            log.debug("카카오 검색 실패 ($url): ${e.message}")
            null
        }
    }
}
