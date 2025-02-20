package com.singhand.sr.graphservice.bizkeycloakmodel.repository;

import com.singhand.sr.graphservice.bizkeycloakmodel.model.UserEntity;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserEntityRepository extends JpaRepository<UserEntity, String>,
    JpaSpecificationExecutor<UserEntity> {

  Collection<UserEntity> findByGroups_Id(String id);

  boolean existsByUsername(String username);

  Optional<UserEntity> findByUsername(String username);

  Optional<UserEntity> findByUsernameAndRealmId(String username, String realmId);

  @Query("""
      select u from UserEntity u
      join u.groups g
      where g.id = :groupId
      """)
  Collection<UserEntity> findByGroups_IdAndUsernameNotLike(String groupId);
}