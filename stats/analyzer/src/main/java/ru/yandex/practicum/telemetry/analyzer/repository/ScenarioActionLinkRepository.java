package ru.yandex.practicum.telemetry.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.telemetry.analyzer.model.ScenarioActionLink;

import java.util.List;

public interface ScenarioActionLinkRepository extends JpaRepository<ScenarioActionLink, Long> {

    List<ScenarioActionLink> findAllByScenarioId(Long scenarioId);
}
