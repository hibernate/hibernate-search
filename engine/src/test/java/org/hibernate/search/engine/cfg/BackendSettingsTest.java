/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
