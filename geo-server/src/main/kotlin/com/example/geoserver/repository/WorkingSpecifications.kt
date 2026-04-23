package com.example.geoserver.repository

import com.example.geoserver.entity.Working
import org.springframework.data.jpa.domain.Specification
import java.time.LocalDate
import org.slf4j.LoggerFactory

/**
 * Спецификации для динамической фильтрации Working по критериям.
 *
 * ПОЧЕМУ SPECIFICATION ВМЕСТО "? IS NULL"?
 * ============================================
 * PostgreSQL требует явной типизации параметров в prepared statements.
 * Конструкция ":param IS NULL" в JPQL может привести к SQLState: 42P18,
 * если параметры не правильно типизированы при трансляции в native SQL.
 *
 * Specification API решает эту проблему:
 * 1. Динамически добавляет условия ТОЛЬКО если параметр не null
 * 2. Не использует конструкцию "IS NULL" в SQL
 * 3. PostgreSQL видит конкретные типы данных (DATE, BIGINT и т.д.)
 * 4. Код становится более читаемым и масштабируемым
 *
 * АРХИТЕКТУРНЫЕ ПРЕИМУЩЕСТВА:
 * - Разделение ответственности: фильтрация отделена от репозитория
 * - Легко добавлять новые фильтры без изменения SQL
 * - Условия строятся динамически, не требуется многовариантность запросов
 * - Типобезопасность: используется JPA Criteria API
 */
object WorkingSpecifications {

    private val logger = LoggerFactory.getLogger(WorkingSpecifications::class.java)

    /**
     * Объединённая спецификация для фильтрации по всем параметрам.
     *
     * Логирует входные параметры для отладки фильтрации.
     */
    fun filterByParameters(
        startDate: LocalDate?,
        endDate: LocalDate?,
        contractorId: Long?,
        areaId: Long?
    ): Specification<Working> {
        logger.info(
            "Filtering working records with startDate=$startDate, endDate=$endDate, " +
            "contractorId=$contractorId, areaId=$areaId"
        )

        return Specification { root, query, cb ->
            val predicates = mutableListOf<jakarta.persistence.criteria.Predicate>()

            // Фильтр по startDate: если параметр не null, добавляем условие w.startDate >= startDate
            startDate?.let {
                val predicate = cb.greaterThanOrEqualTo(root.get("startDate"), it)
                predicates.add(predicate)
                logger.debug("Added startDate filter: startDate >= $it")
            }

            // Фильтр по endDate: если параметр не null, добавляем условие w.endDate <= endDate
            endDate?.let {
                val predicate = cb.lessThanOrEqualTo(root.get("endDate"), it)
                predicates.add(predicate)
                logger.debug("Added endDate filter: endDate <= $it")
            }

            // Фильтр по contractorId: если параметр не null, добавляем условие w.contractor.id = contractorId
            contractorId?.let {
                val predicate = cb.equal(root.get<Any>("contractor").get<Long>("id"), it)
                predicates.add(predicate)
                logger.debug("Added contractor filter: contractor.id = $it")
            }

            // Фильтр по areaId: если параметр не null, добавляем условие w.area.id = areaId
            areaId?.let {
                val predicate = cb.equal(root.get<Any>("area").get<Long>("id"), it)
                predicates.add(predicate)
                logger.debug("Added area filter: area.id = $it")
            }

            logger.info("Total predicates built: ${predicates.size}")

            // Если предикатов нет (все параметры null), возвращаем null для получения всех записей
            if (predicates.isEmpty()) {
                null
            } else {
                // Объединяем все предикаты с AND
                cb.and(*predicates.toTypedArray())
            }
        }
    }

    /**
     * Спецификация для фильтра по дате начала.
     * startDate >= параметр
     */
    fun byStartDateGreaterThanOrEqual(startDate: LocalDate?): Specification<Working> {
        return if (startDate != null) {
            Specification { root, _, cb ->
                cb.greaterThanOrEqualTo(root.get("startDate"), startDate)
            }
        } else {
            Specification { _, _, _ -> null }
        }
    }

    /**
     * Спецификация для фильтра по дате окончания.
     * endDate <= параметр
     */
    fun byEndDateLessThanOrEqual(endDate: LocalDate?): Specification<Working> {
        return if (endDate != null) {
            Specification { root, _, cb ->
                cb.lessThanOrEqualTo(root.get("endDate"), endDate)
            }
        } else {
            Specification { _, _, _ -> null }
        }
    }

    /**
     * Спецификация для фильтра по ID подрядчика.
     * contractor.id = параметр
     */
    fun byContractorId(contractorId: Long?): Specification<Working> {
        return if (contractorId != null) {
            Specification { root, _, cb ->
                cb.equal(root.get<Any>("contractor").get<Long>("id"), contractorId)
            }
        } else {
            Specification { _, _, _ -> null }
        }
    }

    /**
     * Спецификация для фильтра по ID участка.
     * area.id = параметр
     */
    fun byAreaId(areaId: Long?): Specification<Working> {
        return if (areaId != null) {
            Specification { root, _, cb ->
                cb.equal(root.get<Any>("area").get<Long>("id"), areaId)
            }
        } else {
            Specification { _, _, _ -> null }
        }
    }
}
