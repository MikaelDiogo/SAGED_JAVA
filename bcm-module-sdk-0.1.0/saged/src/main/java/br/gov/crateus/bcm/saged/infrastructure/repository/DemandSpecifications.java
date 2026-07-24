package br.gov.crateus.bcm.saged.infrastructure.repository;

import br.gov.crateus.bcm.saged.domain.DemandStatus;
import br.gov.crateus.bcm.saged.infrastructure.entity.DemandEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class DemandSpecifications {

    private DemandSpecifications() {}

    public static Specification<DemandEntity> withStatus(DemandStatus status) {
        return (root, query, cb) ->
            status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<DemandEntity> withSpecialtyId(UUID specialtyId) {
        return (root, query, cb) ->
            specialtyId == null ? null : cb.equal(root.get("specialty").get("id"), specialtyId);
    }

    public static Specification<DemandEntity> withDepartmentId(UUID departmentId) {
        return (root, query, cb) ->
            departmentId == null ? null : cb.equal(root.get("departmentId"), departmentId);
    }

    public static Specification<DemandEntity> withSpecialtyCodeIn(List<String> codes) {
        return (root, query, cb) ->
            (codes == null || codes.isEmpty()) ? null : root.get("specialty").get("code").in(codes);
    }
}
