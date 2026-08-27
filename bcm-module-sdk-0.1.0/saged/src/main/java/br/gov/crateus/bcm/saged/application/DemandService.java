package br.gov.crateus.bcm.saged.application;

import br.gov.crateus.bcm.saged.domain.DemandStatus;
import br.gov.crateus.bcm.saged.infrastructure.entity.DemandEntity;
import br.gov.crateus.bcm.saged.infrastructure.entity.DemandHistoryEntity;
import br.gov.crateus.bcm.saged.infrastructure.entity.SpecialtyEntity;
import br.gov.crateus.bcm.saged.infrastructure.repository.DemandHistoryRepository;
import br.gov.crateus.bcm.saged.infrastructure.repository.DemandRepository;
import br.gov.crateus.bcm.saged.infrastructure.repository.DemandSpecifications;
import br.gov.crateus.bcm.saged.infrastructure.repository.SpecialtyRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DemandService {

    private static final Map<DemandStatus, Set<DemandStatus>> VALID_TRANSITIONS = Map.of(
        DemandStatus.TODO,        Set.of(DemandStatus.IN_PROGRESS),
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
        DemandEntity demand = findById(demandId);

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

    public DemandEntity assign(UUID demandId, UUID assigneeUserId, String actor) {
        DemandEntity demand = findById(demandId);
        demand.setAssigneeUserId(assigneeUserId);
        demand.setUpdatedBy(actor);
        demand = demandRepository.save(demand);
        recordHistory(demand, "ASSIGNED", null, actor);
        return demand;
    }

    public DemandEntity updateNote(UUID demandId, String note, String actor) {
        DemandEntity demand = findById(demandId);
        demand.setCurrentTechnicalNote(note);
        demand.setUpdatedBy(actor);
        demand = demandRepository.save(demand);
        recordHistory(demand, "NOTE_UPDATED", null, actor);
        return demand;
    }

    @Transactional(readOnly = true)
    public Page<DemandEntity> list(String role, UUID orgUnitId, List<String> specialtyCodes,
                                    DemandStatus statusFilter, UUID specialtyIdFilter,
                                    UUID departmentIdFilter, Pageable pageable) {
        Specification<DemandEntity> visibility = switch (role) {
            case "SAGED_ADMIN_SETOR" -> DemandSpecifications.withDepartmentId(orgUnitId);
            case "SAGED_TECNICO"     -> DemandSpecifications.withSpecialtyCodeIn(specialtyCodes);
            default                  -> Specification.where(null);
        };

        Specification<DemandEntity> filters = Specification
            .where(DemandSpecifications.withStatus(statusFilter))
            .and(DemandSpecifications.withSpecialtyId(specialtyIdFilter))
            .and(DemandSpecifications.withDepartmentId(departmentIdFilter));

        return demandRepository.findAll(visibility.and(filters), pageable);
    }

    @Transactional(readOnly = true)
    public DemandEntity findById(UUID id) {
        return demandRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Demand not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<DemandEntity> listByRequester(UUID requesterUserId) {
        return demandRepository.findByRequesterUserId(requesterUserId);
    }

    @Transactional(readOnly = true)
    public Optional<DemandEntity> findByProtocol(String protocol) {
        return demandRepository.findByProtocol(protocol);
    }

    public DemandEntity updateEquipment(UUID demandId, Boolean isRented, String assetTag,
                                         String equipmentName, String equipmentModel, String actor) {
        DemandEntity demand = findById(demandId);
        demand.setIsRented(isRented);
        if (assetTag != null) demand.setAssetTag(assetTag.isBlank() ? null : assetTag.trim());
        if (equipmentName != null) demand.setEquipmentName(equipmentName.isBlank() ? null : equipmentName.trim());
        if (equipmentModel != null) demand.setEquipmentModel(equipmentModel.isBlank() ? null : equipmentModel.trim());
        demand.setUpdatedBy(actor);
        demand = demandRepository.save(demand);
        recordHistory(demand, "EQUIPMENT_UPDATED", null, actor);
        return demand;
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
