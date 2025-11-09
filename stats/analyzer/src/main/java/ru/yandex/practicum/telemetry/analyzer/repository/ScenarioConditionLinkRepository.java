package ru.yandex.practicum.telemetry.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.telemetry.analyzer.model.ScenarioConditionLink;

import java.util.List;

public interface ScenarioConditionLinkRepository extends JpaRepository<ScenarioConditionLink, Long> {

    List<ScenarioConditionLink> findByScenarioId(Long scenarioId);
}
