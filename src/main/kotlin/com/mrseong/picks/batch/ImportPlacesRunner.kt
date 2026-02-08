package com.mrseong.picks.batch

import com.mrseong.picks.domain.place.entity.Place
import com.mrseong.picks.domain.place.entity.PlaceType
import com.mrseong.picks.domain.place.repository.PlaceRepository
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
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

                Thread.sleep(200)
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
        val addressResult = searchByAddress(address)
        if (addressResult != null) return addressResult

        return searchByKeyword("$name $address")
    }

    @Suppress("UNCHECKED_CAST")
    private fun searchByAddress(query: String): Pair<Double, Double>? {
        return try {
            val url = "https://dapi.kakao.com/v2/local/search/address.json?query={query}"
            val headers = org.springframework.http.HttpHeaders().apply {
                set("Authorization", "KakaoAK $kakaoApiKey")
            }
            val entity = org.springframework.http.HttpEntity<Void>(headers)
            val response = restTemplate.exchange(
                url, org.springframework.http.HttpMethod.GET, entity,
                Map::class.java, query
            )
            val documents = response.body?.get("documents") as? List<Map<String, Any>>
            if (!documents.isNullOrEmpty()) {
                val doc = documents[0]
                val lat = (doc["y"] as String).toDouble()
                val lng = (doc["x"] as String).toDouble()
                Pair(lat, lng)
            } else null
        } catch (e: Exception) {
            log.debug("주소 검색 실패: ${e.message}")
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun searchByKeyword(query: String): Pair<Double, Double>? {
        return try {
            val url = "https://dapi.kakao.com/v2/local/search/keyword.json?query={query}"
            val headers = org.springframework.http.HttpHeaders().apply {
                set("Authorization", "KakaoAK $kakaoApiKey")
            }
            val entity = org.springframework.http.HttpEntity<Void>(headers)
            val response = restTemplate.exchange(
                url, org.springframework.http.HttpMethod.GET, entity,
                Map::class.java, query
            )
            val documents = response.body?.get("documents") as? List<Map<String, Any>>
            if (!documents.isNullOrEmpty()) {
                val doc = documents[0]
                val lat = (doc["y"] as String).toDouble()
                val lng = (doc["x"] as String).toDouble()
                Pair(lat, lng)
            } else null
        } catch (e: Exception) {
            log.debug("키워드 검색 실패: ${e.message}")
            null
        }
    }
}
