package com.singhand.sr.graphservice.bizmodel.repository.jpa;

import com.singhand.sr.graphservice.bizmodel.model.jpa.Ontology;
import com.singhand.sr.graphservice.bizmodel.model.jpa.OntologyProperty;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OntologyPropertyRepository extends BaseRepository<OntologyProperty, Long> {

  @Query("select distinct name from OntologyProperty")
  Set<String> findAllNames();

  /**
   * 根据本体获取属性
   *
   * @param ontology 实体类型
   * @param pageable 分页
   * @return 本体属性列表
   */
  Page<OntologyProperty> findByOntology(Ontology ontology, Pageable pageable);

  /**
   * 根据本体和属性名获取属性
   *
   * @param ontology 实体类型
   * @param name     属性名
   * @return 本体属性
   */
  Optional<OntologyProperty> findByOntologyAndName(Ontology ontology, String name);

  /**
   * 根据本体名称和属性名获取属性
   *
   * @param ontologyName 本体名称
   * @param name         属性名
   * @return 本体属性
   */
  Optional<OntologyProperty> findByOntology_NameAndName(String ontologyName, String name);

  /**
   * 根据本体名称获取属性
   *
   * @param name 本体名称
   * @return 本体属性列表
   */
  Set<OntologyProperty> findByOntology_Name(String name);

  /**
   * 判断本体属性是否存在
   *
   * @param ontologyName 本体名称
   * @param name         属性名
   * @return 是否存在
   */
  boolean existsByOntology_NameAndName(String ontologyName, String name);

  /**
   * 判断本体属性是否存在
   *
   * @param ontology 本体
   * @param name     属性名
   * @return 是否存在
   */
  boolean existsByOntologyAndName(Ontology ontology, String name);
}
