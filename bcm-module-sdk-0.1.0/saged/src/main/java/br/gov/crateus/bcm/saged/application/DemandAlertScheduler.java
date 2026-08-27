package br.gov.crateus.bcm.saged.application;

import br.gov.crateus.bcm.saged.infrastructure.entity.DemandEntity;
import br.gov.crateus.bcm.saged.infrastructure.entity.SystemNotificationEntity;
import br.gov.crateus.bcm.saged.infrastructure.repository.DemandRepository;
import br.gov.crateus.bcm.saged.infrastructure.repository.SystemNotificationRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemandAlertScheduler {

    private static final Logger log = LoggerFactory.getLogger(DemandAlertScheduler.class);
    private static final String TYPE_UNATTENDED = "DEMAND_UNATTENDED_7D";

    private final DemandRepository demandRepository;
    private final SystemNotificationRepository notificationRepository;

    public DemandAlertScheduler(DemandRepository demandRepository,
                                 SystemNotificationRepository notificationRepository) {
        this.demandRepository = demandRepository;
        this.notificationRepository = notificationRepository;
    }

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void alertUnattendedDemands() {
        OffsetDateTime threshold = OffsetDateTime.now(ZoneOffset.UTC).minusDays(7);
        List<DemandEntity> unattended = demandRepository.findUnattendedWithoutAlert(threshold, TYPE_UNATTENDED);

        for (DemandEntity demand : unattended) {
            SystemNotificationEntity notification = new SystemNotificationEntity();
            notification.setMessage("A demanda " + demand.getProtocol() + " — " + demand.getTitle()
                + " está há mais de 7 dias sem técnico responsável.");
            notification.setType(TYPE_UNATTENDED);
            notification.setRead(false);
            notification.setDemandId(demand.getId());
            notification.setDemandProtocol(demand.getProtocol());
            notification.setRecipientUserId(null); // global: visible to all
            notification.setCreatedBy("scheduler");
            notification.setUpdatedBy("scheduler");
            notificationRepository.save(notification);
            log.info("Alert created for unattended demand: {}", demand.getProtocol());
        }
    }
}
