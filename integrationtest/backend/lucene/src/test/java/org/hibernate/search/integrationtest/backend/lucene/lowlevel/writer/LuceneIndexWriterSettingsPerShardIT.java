/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.lucene.lowlevel.writer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.hibernate.search.backend.lucene.cfg.LuceneIndexSettings;
import org.hibernate.search.backend.lucene.index.impl.LuceneIndexManagerImpl;
import org.hibernate.search.backend.lucene.index.impl.Shard;
import org.hibernate.search.backend.lucene.lowlevel.index.impl.IndexAccessorImpl;
import org.hibernate.search.integrationtest.backend.lucene.sharding.AbstractSettingsPerShardIT;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.LiveIndexWriterConfig;

@TestForIssue(jiraKey = "HSEARCH-3636")
class LuceneIndexWriterSettingsPerShardIT extends AbstractSettingsPerShardIT {

	@Test
	void test() {
		setupHelper.start( setupStrategyFunction ).withIndex( index )
				.withIndexProperty( index.name(), LuceneIndexSettings.IO_WRITER_MAX_BUFFERED_DOCS, "420" )
				.withIndexProperty( index.name(),
						"shards." + shardIds.get( 2 ) + "." + LuceneIndexSettings.IO_WRITER_MAX_BUFFERED_DOCS, "42" )
				.withIndexProperty( index.name(),
						"shards." + shardIds.get( 3 ) + "." + LuceneIndexSettings.IO_WRITER_MAX_BUFFERED_DOCS, "4200" )
				.setup();

		LuceneIndexManagerImpl luceneIndexManager = index.unwrapForTests( LuceneIndexManagerImpl.class );
		assertThat( luceneIndexManager.getShardsForTests() )
				.element( 0 )
				.extracting( Shard::indexAccessorForTests )
				.extracting( this::getWriterForTests )
				.extracting( IndexWriter::getConfig )
				.returns( 420, LiveIndexWriterConfig::getMaxBufferedDocs );
		assertThat( luceneIndexManager.getShardsForTests() )
				.element( 1 )
				.extracting( Shard::indexAccessorForTests )
				.extracting( this::getWriterForTests )
				.extracting( IndexWriter::getConfig )
				.returns( 420, LiveIndexWriterConfig::getMaxBufferedDocs );
		assertThat( luceneIndexManager.getShardsForTests() )
				.element( 2 )
				.extracting( Shard::indexAccessorForTests )
				.extracting( this::getWriterForTests )
				.extracting( IndexWriter::getConfig )
				.returns( 42, LiveIndexWriterConfig::getMaxBufferedDocs );
		assertThat( luceneIndexManager.getShardsForTests() )
				.element( 3 )
				.extracting( Shard::indexAccessorForTests )
				.extracting( this::getWriterForTests )
				.extracting( IndexWriter::getConfig )
				.returns( 4200, LiveIndexWriterConfig::getMaxBufferedDocs );
	}

	private IndexWriter getWriterForTests(IndexAccessorImpl accessor) {
		try {
			return accessor.getWriterForTests();
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}
}
