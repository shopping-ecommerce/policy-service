package iuh.fit.se.repository;

import iuh.fit.se.entity.PolicyVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PolicyVersionRepository extends JpaRepository<PolicyVersion, String> {

    List<PolicyVersion> findByPolicyIdOrderByCreatedAtDesc(String policyId);

    Optional<PolicyVersion> findByPolicyIdAndVersion(String policyId, String version);

    // bản đang hiệu lực tại ngày at
    @Query("""
      select pv from PolicyVersion pv
      where pv.policyId = :policyId
        and pv.status = 'PUBLISHED'
        and pv.startDate <= :at
        and (pv.endDate is null or pv.endDate >= :at)
      order by pv.startDate desc
    """)
    Optional<PolicyVersion> findEffectiveAt(String policyId, LocalDate at);

    // tất cả bản published, mới nhất trước
    @Query("""
      select pv from PolicyVersion pv
      where pv.policyId = :policyId
        and pv.status = 'PUBLISHED'
      order by pv.startDate desc, pv.publishedAt desc
    """)
    List<PolicyVersion> findPublishedDesc(String policyId);

    // một draft duy nhất (nếu có)
    @Query("""
      select pv from PolicyVersion pv
      where pv.policyId = :policyId
        and pv.status = 'DRAFT'
      order by pv.createdAt desc
    """)
    List<PolicyVersion> findDrafts(String policyId);

    // phục vụ autoNextVersion
    @Query("""
      select pv from PolicyVersion pv
      where pv.policyId = :policyId
      order by pv.createdAt desc
    """)
    List<PolicyVersion> findAllByPolicyIdOrderByCreatedAtDesc(String policyId);
}
