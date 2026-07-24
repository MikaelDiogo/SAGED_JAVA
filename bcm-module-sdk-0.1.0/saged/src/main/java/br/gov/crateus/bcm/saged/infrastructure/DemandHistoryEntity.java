package br.gov.crateus.bcm.saged.infrastructure;

import br.gov.crateus.bcm.devhost.persistence.SdkAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(schema = "saged", name = "demand_history")
public class DemandHistoryEntity extends SdkAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demand_id", nullable = false)
    private DemandEntity demand;

    @Column(name = "action", length = 64, nullable = false)
    private String action;

    @Column(name = "justification", columnDefinition = "TEXT")
    private String justification;

    public DemandEntity getDemand() { return demand; }
    public void setDemand(DemandEntity demand) { this.demand = demand; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }
}
