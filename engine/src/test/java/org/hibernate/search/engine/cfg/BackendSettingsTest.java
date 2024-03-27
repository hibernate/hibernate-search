/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.cfg;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BackendSettingsTest {

	@Test
	void backendKey() {
		assertThat( BackendSettings.backendKey( "foo.bar" ) )
				.isEqualTo( "hibernate.search.backend.foo.bar" );

		assertThat( BackendSettings.backendKey( "myBackend", "foo.bar" ) )
				.isEqualTo( "hibernate.search.backends.myBackend.foo.bar" );
		assertThat( BackendSettings.backendKey( null, "foo.bar" ) )
				.isEqualTo( "hibernate.search.backend.foo.bar" );
	}

}
