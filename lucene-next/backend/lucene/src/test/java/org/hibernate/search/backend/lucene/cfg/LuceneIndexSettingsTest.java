/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.cfg;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LuceneIndexSettingsTest {

	@Test
	void shardKey() {
		assertThat( LuceneIndexSettings.shardKey( "shardId", "foo.bar" ) )
				.isEqualTo( "hibernate.search.backend.shards.shardId.foo.bar" );

		assertThat( LuceneIndexSettings.shardKey( "indexName", "shardId", "foo.bar" ) )
				.isEqualTo( "hibernate.search.backend.indexes.indexName.shards.shardId.foo.bar" );
		assertThat( LuceneIndexSettings.shardKey( null, "shardId", "foo.bar" ) )
				.isEqualTo( "hibernate.search.backend.shards.shardId.foo.bar" );

		assertThat( LuceneIndexSettings.shardKey( "backendName", "indexName", "shardId", "foo.bar" ) )
				.isEqualTo( "hibernate.search.backends.backendName.indexes.indexName.shards.shardId.foo.bar" );
		assertThat( LuceneIndexSettings.shardKey( null, "indexName", "shardId", "foo.bar" ) )
				.isEqualTo( "hibernate.search.backend.indexes.indexName.shards.shardId.foo.bar" );
		assertThat( LuceneIndexSettings.shardKey( "backendName", null, "shardId", "foo.bar" ) )
				.isEqualTo( "hibernate.search.backends.backendName.shards.shardId.foo.bar" );
	}

}
