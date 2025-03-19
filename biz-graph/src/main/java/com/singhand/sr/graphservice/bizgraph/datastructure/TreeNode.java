package com.singhand.sr.graphservice.bizgraph.datastructure;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TreeNode {

  private String name;

  private Set<TreeNode> children = new HashSet<>();

  private Set<String> attributes = new HashSet<>();

  public Set<TreeNode> getPosterity() {

    if (children == null) {
      return Set.of();
    }
    final var nodes = new HashSet<>(children);
    children.forEach(it -> nodes.addAll(it.getPosterity()));
    return nodes;
  }

  public Optional<TreeNode> findFirst(String name) {

    if (this.name.equals(name)) {
      return Optional.of(this);
    } else if (children != null) {
      for (final var node : children) {
        final var child = node.findFirst(name);
        if (child.isPresent()) {
          return child;
        }
      }
    }
    return Optional.empty();
  }
}
