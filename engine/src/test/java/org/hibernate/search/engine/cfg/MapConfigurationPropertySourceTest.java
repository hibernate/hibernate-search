/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.cfg;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.engine.cfg.spi.AllAwareConfigurationPropertySource;

import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
class MapConfigurationPropertySourceTest extends AbstractAllAwareConfigurationPropertySourceTest {

	@Test
	void to_string() {
		ConfigurationPropertySource propertySource = createPropertySource( "someKey", "foo" );
		assertThat( propertySource ).asString()
				.contains( "Map" )
				.contains( "someKey=foo" );
	}

	@Test
	void get_integer() {
		ConfigurationPropertySource propertySource = AllAwareConfigurationPropertySource.fromMap(
				Collections.singletonMap( "someKey", 42 )
		);
		assertThat( (Optional<Object>) propertySource.get( "someKey" ) ).contains( 42 );
	}

	@Override
	protected AllAwareConfigurationPropertySource createPropertySource(Map<String, String> content) {
		return AllAwareConfigurationPropertySource.fromMap( content );
	}
}
