package com.singhand.sr.graphservice.bizkeycloakmodel.repository;

import com.singhand.sr.graphservice.bizkeycloakmodel.model.EventEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface EventEntityRepository extends JpaRepository<EventEntity, String> {

  @Transactional(readOnly = true)
  Optional<EventEntity> findFirstByUserIdAndTypeIsOrderByEventTimeDesc(String userId, String type);

  @Transactional(readOnly = true)
  Page<EventEntity> findByTypeIn(Collection<String> types, Pageable pageable);

  @Transactional(readOnly = true)
  Page<EventEntity> findByType(String type, Pageable pageable);

  @Transactional(readOnly = true)
  @Query("""
      select e.ipAddress, count(e) as c from EventEntity e
      where e.userId = :userId and e.type in ('LOGIN', 'LOGIN_ERROR')
      group by e.ipAddress
      order by c desc
      """)
  List<Object[]> countByIpAddress(String userId);
}