/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IndexSettingsTest {

	@Test
	void indexKey() {
		assertThat( IndexSettings.indexKey( "indexName", "foo.bar" ) )
				.isEqualTo( "hibernate.search.backend.indexes.indexName.foo.bar" );

		assertThat( IndexSettings.indexKey( "backendName", "indexName", "foo.bar" ) )
				.isEqualTo( "hibernate.search.backends.backendName.indexes.indexName.foo.bar" );
		assertThat( IndexSettings.indexKey( null, "indexName", "foo.bar" ) )
				.isEqualTo( "hibernate.search.backend.indexes.indexName.foo.bar" );
	}
}
