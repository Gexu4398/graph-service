package com.singhand.sr.graphservice.bizgraph.config.yaml;

import java.util.Objects;
import lombok.NonNull;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

public class YamlPropertySourceFactory implements PropertySourceFactory {

  @Override
  public @NonNull PropertySource<?> createPropertySource(String name,
      EncodedResource encodedResource) {

    final var factory = new YamlPropertiesFactoryBean();
    factory.setResources(encodedResource.getResource());
    final var properties = factory.getObject();
    assert properties != null;
    return new PropertiesPropertySource(
        Objects.requireNonNull(encodedResource.getResource().getFilename()), properties);
  }
}
