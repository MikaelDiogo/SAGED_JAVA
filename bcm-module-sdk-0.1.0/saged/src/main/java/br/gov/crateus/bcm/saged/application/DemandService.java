package br.gov.crateus.bcm.saged.application;

import br.gov.crateus.bcm.saged.domain.DemandStatus;
import br.gov.crateus.bcm.saged.infrastructure.DemandEntity;
import br.gov.crateus.bcm.saged.infrastructure.DemandHistoryEntity;
import br.gov.crateus.bcm.saged.infrastructure.DemandHistoryRepository;
import br.gov.crateus.bcm.saged.infrastructure.DemandRepository;
import br.gov.crateus.bcm.saged.infrastructure.SpecialtyEntity;
import br.gov.crateus.bcm.saged.infrastructure.SpecialtyRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DemandService {

    private static final Map<DemandStatus, Set<DemandStatus>> VALID_TRANSITIONS = Map.of(
        DemandStatus.TODO, Set.of(DemandStatus.IN_PROGRESS),
        DemandStatus.IN_PROGRESS, Set.of(DemandStatus.DONE, DemandStatus.INTERRUPTED)
    );

    private final DemandRepository demandRepository;
    private final DemandHistoryRepository historyRepository;
    private final SpecialtyRepository specialtyRepository;

    public DemandService(DemandRepository demandRepository,
                         DemandHistoryRepository historyRepository,
                         SpecialtyRepository specialtyRepository) {
        this.demandRepository = demandRepository;
        this.historyRepository = historyRepository;
        this.specialtyRepository = specialtyRepository;
    }

    public DemandEntity create(String title, String description, String specialtyCode,
                                String assetTag, UUID requesterUserId, UUID departmentId,
                                String actor) {
        SpecialtyEntity specialty = specialtyRepository.findWithLockByCode(specialtyCode)
            .orElseThrow(() -> new IllegalArgumentException("Specialty not found: " + specialtyCode));

        int year = LocalDate.now().getYear();
        long count = demandRepository.countBySpecialtyIdAndYear(specialty.getId(), year);
        String protocol = String.format("%d-%s-%05d", year, specialty.getCode(), count + 1);

        DemandEntity demand = new DemandEntity();
        demand.setProtocol(protocol);
        demand.setTitle(title);
        demand.setDescription(description);
        demand.setStatus(DemandStatus.TODO);
        demand.setSpecialty(specialty);
        demand.setRequesterUserId(requesterUserId);
        demand.setDepartmentId(departmentId);
        demand.setAssetTag(assetTag);
        demand.setCreatedBy(actor);
        demand.setUpdatedBy(actor);

        demand = demandRepository.save(demand);
        recordHistory(demand, "CREATED", null, actor);
        return demand;
    }

    public DemandEntity changeStatus(UUID demandId, DemandStatus newStatus,
                                      String justification, String actor) {
        DemandEntity demand = demandRepository.findById(demandId)
            .orElseThrow(() -> new IllegalArgumentException("Demand not found: " + demandId));

        Set<DemandStatus> allowed = VALID_TRANSITIONS.getOrDefault(demand.getStatus(), Set.of());
        if (!allowed.contains(newStatus)) {
            throw new IllegalStateException(
                "Transition " + demand.getStatus() + " -> " + newStatus + " is not allowed");
        }

        if (newStatus == DemandStatus.INTERRUPTED && (justification == null || justification.isBlank())) {
            throw new IllegalArgumentException("Justification is required when interrupting a demand");
        }

        demand.setStatus(newStatus);
        demand.setUpdatedBy(actor);
        demand = demandRepository.save(demand);
        recordHistory(demand, newStatus.name(), justification, actor);
        return demand;
    }

    @Transactional(readOnly = true)
    public List<DemandEntity> list(String role, UUID orgUnitId, List<String> specialtyCodes) {
        return switch (role) {
            case "SAGED_ADMIN_GERAL" -> demandRepository.findAll();
            case "SAGED_ADMIN_SETOR" -> orgUnitId != null
                ? demandRepository.findByDepartmentId(orgUnitId)
                : List.of();
            default -> specialtyCodes != null && !specialtyCodes.isEmpty()
                ? demandRepository.findBySpecialtyCodeIn(specialtyCodes)
                : List.of();
        };
    }

    @Transactional(readOnly = true)
    public List<DemandHistoryEntity> getHistory(UUID demandId) {
        if (!demandRepository.existsById(demandId)) {
            throw new IllegalArgumentException("Demand not found: " + demandId);
        }
        return historyRepository.findByDemandIdOrdered(demandId);
    }

    private void recordHistory(DemandEntity demand, String action, String justification, String actor) {
        DemandHistoryEntity h = new DemandHistoryEntity();
        h.setDemand(demand);
        h.setAction(action);
        h.setJustification(justification);
        h.setCreatedBy(actor);
        h.setUpdatedBy(actor);
        historyRepository.save(h);
    }
}
