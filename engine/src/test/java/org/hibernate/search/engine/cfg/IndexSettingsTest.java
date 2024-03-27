/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
