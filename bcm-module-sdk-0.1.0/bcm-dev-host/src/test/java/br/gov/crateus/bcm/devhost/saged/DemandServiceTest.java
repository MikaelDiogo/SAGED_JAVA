package br.gov.crateus.bcm.devhost.saged;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gov.crateus.bcm.saged.application.DemandService;
import br.gov.crateus.bcm.saged.domain.DemandStatus;
import br.gov.crateus.bcm.saged.infrastructure.entity.DemandEntity;
import br.gov.crateus.bcm.saged.infrastructure.entity.DemandHistoryEntity;
import br.gov.crateus.bcm.saged.infrastructure.entity.SpecialtyEntity;
import br.gov.crateus.bcm.saged.infrastructure.repository.DemandHistoryRepository;
import br.gov.crateus.bcm.saged.infrastructure.repository.DemandRepository;
import br.gov.crateus.bcm.saged.infrastructure.repository.SpecialtyRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DemandServiceTest {

    @Mock DemandRepository demandRepository;
    @Mock DemandHistoryRepository historyRepository;
    @Mock SpecialtyRepository specialtyRepository;

    DemandService service;

    @BeforeEach
    void setUp() {
        service = new DemandService(demandRepository, historyRepository, specialtyRepository);
    }

    @Test
    void create_generatesCorrectProtocol() {
        SpecialtyEntity specialty = specialty("HW");
        int year = LocalDate.now().getYear();

        when(specialtyRepository.findWithLockByCode("HW")).thenReturn(Optional.of(specialty));
        when(demandRepository.countBySpecialtyIdAndYear(specialty.getId(), year)).thenReturn(3L);
        when(demandRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DemandEntity result = service.create("Title", "Desc", "HW", null,
                UUID.randomUUID(), UUID.randomUUID(), "actor");

        assertThat(result.getProtocol()).isEqualTo(year + "-HW-00004");
        assertThat(result.getStatus()).isEqualTo(DemandStatus.TODO);
    }

    @Test
    void create_recordsCreatedHistory() {
        SpecialtyEntity specialty = specialty("NET");
        when(specialtyRepository.findWithLockByCode("NET")).thenReturn(Optional.of(specialty));
        when(demandRepository.countBySpecialtyIdAndYear(any(), any(int.class))).thenReturn(0L);
        when(demandRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.create("Title", "Desc", "NET", null, UUID.randomUUID(), UUID.randomUUID(), "actor");

        ArgumentCaptor<DemandHistoryEntity> captor = ArgumentCaptor.forClass(DemandHistoryEntity.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo("CREATED");
    }

    @Test
    void create_throwsWhenSpecialtyNotFound() {
        when(specialtyRepository.findWithLockByCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create("T", "D", "UNKNOWN", null,
                UUID.randomUUID(), UUID.randomUUID(), "actor"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Specialty not found");
    }

    @Test
    void changeStatus_validTransition_TODO_to_IN_PROGRESS() {
        DemandEntity demand = demand(DemandStatus.TODO);
        when(demandRepository.findById(demand.getId())).thenReturn(Optional.of(demand));
        when(demandRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DemandEntity result = service.changeStatus(demand.getId(), DemandStatus.IN_PROGRESS, null, "actor");

        assertThat(result.getStatus()).isEqualTo(DemandStatus.IN_PROGRESS);

        ArgumentCaptor<DemandHistoryEntity> captor = ArgumentCaptor.forClass(DemandHistoryEntity.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo("IN_PROGRESS");
    }

    @Test
    void changeStatus_invalidTransition_throwsIllegalStateException() {
        DemandEntity demand = demand(DemandStatus.TODO);
        when(demandRepository.findById(demand.getId())).thenReturn(Optional.of(demand));

        assertThatThrownBy(() -> service.changeStatus(demand.getId(), DemandStatus.DONE, null, "actor"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void changeStatus_terminalState_noFurtherTransitions() {
        DemandEntity demand = demand(DemandStatus.DONE);
        when(demandRepository.findById(demand.getId())).thenReturn(Optional.of(demand));

        assertThatThrownBy(() -> service.changeStatus(demand.getId(), DemandStatus.IN_PROGRESS, null, "actor"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void changeStatus_interruptedWithoutJustification_throwsIllegalArgumentException() {
        DemandEntity demand = demand(DemandStatus.IN_PROGRESS);
        when(demandRepository.findById(demand.getId())).thenReturn(Optional.of(demand));

        assertThatThrownBy(() -> service.changeStatus(demand.getId(), DemandStatus.INTERRUPTED, "", "actor"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Justification is required");
    }

    @Test
    void changeStatus_interruptedWithJustification_succeeds() {
        DemandEntity demand = demand(DemandStatus.IN_PROGRESS);
        when(demandRepository.findById(demand.getId())).thenReturn(Optional.of(demand));
        when(demandRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DemandEntity result = service.changeStatus(demand.getId(), DemandStatus.INTERRUPTED, "hardware failure", "actor");

        assertThat(result.getStatus()).isEqualTo(DemandStatus.INTERRUPTED);
    }

    @Test
    void assign_setsAssigneeAndRecordsHistory() {
        DemandEntity demand = demand(DemandStatus.IN_PROGRESS);
        UUID assignee = UUID.randomUUID();
        when(demandRepository.findById(demand.getId())).thenReturn(Optional.of(demand));
        when(demandRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DemandEntity result = service.assign(demand.getId(), assignee, "actor");

        assertThat(result.getAssigneeUserId()).isEqualTo(assignee);

        ArgumentCaptor<DemandHistoryEntity> captor = ArgumentCaptor.forClass(DemandHistoryEntity.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo("ASSIGNED");
    }

    @Test
    void updateNote_setsNoteAndRecordsHistory() {
        DemandEntity demand = demand(DemandStatus.IN_PROGRESS);
        when(demandRepository.findById(demand.getId())).thenReturn(Optional.of(demand));
        when(demandRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DemandEntity result = service.updateNote(demand.getId(), "replaced the power cable", "actor");

        assertThat(result.getCurrentTechnicalNote()).isEqualTo("replaced the power cable");

        ArgumentCaptor<DemandHistoryEntity> captor = ArgumentCaptor.forClass(DemandHistoryEntity.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo("NOTE_UPDATED");
    }

    @Test
    void findById_throwsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(demandRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Demand not found");
    }

    // --- helpers ---

    private SpecialtyEntity specialty(String code) {
        SpecialtyEntity s = new SpecialtyEntity();
        s.setCode(code);
        s.setName(code);
        return s;
    }

    private DemandEntity demand(DemandStatus status) {
        DemandEntity d = new DemandEntity();
        d.setStatus(status);
        d.setTitle("Test demand");
        d.setDescription("Test description");
        d.setRequesterUserId(UUID.randomUUID());
        d.setDepartmentId(UUID.randomUUID());
        return d;
    }
}
