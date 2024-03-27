/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.mapping;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotatedTypeSource;
import org.hibernate.search.mapper.pojo.standalone.bootstrap.spi.StandalonePojoIntegrationBooter;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.reflect.spi.ValueHandleFactory;

@Incubating
public final class SearchMappingBuilder {

	private final StandalonePojoIntegrationBooter.Builder booterBuilder;

	SearchMappingBuilder(AnnotatedTypeSource annotatedTypeSource) {
		booterBuilder = StandalonePojoIntegrationBooter.builder()
				.annotatedTypeSource( annotatedTypeSource );
	}

	/* package-protected */ SearchMappingBuilder valueReadHandleFactory(ValueHandleFactory valueHandleFactory) {
		booterBuilder.valueReadHandleFactory( valueHandleFactory );
		return this;
	}

	/**
	 * Sets a configuration property.
	 * <p>
	 * Configuration properties are mentioned in {@link org.hibernate.search.mapper.pojo.standalone.cfg.StandalonePojoMapperSettings},
	 * or in the reference documentation for backend-related properties.
	 *
	 * @param name The name (key) of the configuration property.
	 * @param value The value of the configuration property.
	 * @return {@code this}, for call chaining.
	 */
	public SearchMappingBuilder property(String name, Object value) {
		booterBuilder.property( name, value );
		return this;
	}

	/**
	 * Sets multiple configuration properties.
	 * <p>
	 * Configuration properties are mentioned in {@link org.hibernate.search.mapper.pojo.standalone.cfg.StandalonePojoMapperSettings},
	 * or in the reference documentation for backend-related properties.
	 *
	 * @param map A map containing property names (property keys) as map keys and property values as map values.
	 * @return {@code this}, for call chaining.
	 */
	public SearchMappingBuilder properties(Map<String, ?> map) {
		booterBuilder.properties( map );
		return this;
	}


	/**
	 * Reads the properties from the reader and sets them as overrides for already configured properties.
	 * <p>
	 * Provided reader should be compatible with {@link java.util.Properties#load(Reader)}.
	 * <p>
	 * Configuration properties are mentioned in {@link org.hibernate.search.mapper.pojo.standalone.cfg.StandalonePojoMapperSettings},
	 * or in the reference documentation for backend-related properties.
	 *
	 * @param propertiesReader A configuration property source reader.
	 * Properties from it will be added as an override to previously set properties.
	 * @return {@code this}, for call chaining.
	 * @see ConfigurationPropertySource#withOverride(ConfigurationPropertySource)
	 */
	public SearchMappingBuilder properties(Reader propertiesReader) throws IOException {
		Properties loaded = new Properties();
		loaded.load( propertiesReader );

		properties(
				loaded.entrySet()
						.stream()
						.collect( Collectors.toMap( e -> Objects.toString( e.getKey() ), Map.Entry::getValue ) )
		);

		return this;
	}

	/**
	 * Builds the search mapping.
	 * @return The {@link SearchMapping}.
	 */
	public CloseableSearchMapping build() {
		return booterBuilder.build().boot();
	}
}
