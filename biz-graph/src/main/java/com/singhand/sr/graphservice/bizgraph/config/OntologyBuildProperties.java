package com.singhand.sr.graphservice.bizgraph.config;

import com.singhand.sr.graphservice.bizgraph.config.yaml.YamlPropertySourceFactory;
import com.singhand.sr.graphservice.bizgraph.datastructure.TreeNode;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ConfigurationProperties(prefix = "ontology-build")
@PropertySource(value = "classpath:ontology-build.yml", factory = YamlPropertySourceFactory.class)
@Getter
@Setter
public class OntologyBuildProperties {

  private Set<TreeNode> types;

  public List<String> asList() {

    final var root = new TreeNode();
    root.setName("root");
    root.setChildren(types);
    return root.getPosterity().stream().map(TreeNode::getName).toList();
  }

  public Set<String> asSet() {

    return new HashSet<>(asList());
  }

  public Optional<TreeNode> findFirst(String name) {

    final var root = new TreeNode();
    root.setName("root");
    root.setChildren(types);
    return root.findFirst(name);
  }
}
