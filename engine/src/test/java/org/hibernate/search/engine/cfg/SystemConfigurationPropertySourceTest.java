/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.cfg;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.search.engine.cfg.spi.AllAwareConfigurationPropertySource;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.impl.test.SystemHelper;
import org.hibernate.search.util.impl.test.SystemHelper.SystemPropertyRestorer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SystemConfigurationPropertySourceTest extends AbstractAllAwareConfigurationPropertySourceTest {
	private final List<SystemPropertyRestorer> toClose = new ArrayList<>();

	@Test
	void to_string() {
		ConfigurationPropertySource propertySource = createPropertySource( "someKey", "foo" );
		assertThat( propertySource ).asString().contains( "System" );
	}

	@AfterEach
	void restoreSystemProperties() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( SystemPropertyRestorer::close, toClose );
		}
	}

	@Override
	protected AllAwareConfigurationPropertySource createPropertySource(Map<String, String> content) {
		restoreSystemProperties();
		for ( Map.Entry<String, String> entry : content.entrySet() ) {
			String key = entry.getKey();
			String value = entry.getValue();
			toClose.add( SystemHelper.setSystemProperty( key, value ) );
		}
		return AllAwareConfigurationPropertySource.system();
	}
}
