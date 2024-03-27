/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.lucene.lowlevel.directory;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.hibernate.search.backend.lucene.index.impl.LuceneIndexManagerImpl;
import org.hibernate.search.backend.lucene.index.impl.Shard;
import org.hibernate.search.backend.lucene.lowlevel.index.impl.IndexAccessorImpl;
import org.hibernate.search.integrationtest.backend.lucene.sharding.AbstractSettingsPerShardIT;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.FSDirectory;

@TestForIssue(jiraKey = "HSEARCH-3636")
class LuceneDirectorySettingsPerShardIT extends AbstractSettingsPerShardIT {

	@TempDir
	public Path temporaryFolder;

	@TempDir
	public Path anotherTemporaryFolder;

	@Test
	void test() {
		Path shard0Directory = temporaryFolder.resolve( index.name() ).resolve( shardIds.get( 0 ) );
		Path shard1Directory = temporaryFolder.resolve( index.name() ).resolve( shardIds.get( 1 ) );
		Path shard2Directory = anotherTemporaryFolder.resolve( index.name() ).resolve( shardIds.get( 2 ) );

		assertThat( shard0Directory ).doesNotExist();
		assertThat( shard1Directory ).doesNotExist();
		assertThat( shard2Directory ).doesNotExist();

		setupHelper.start( setupStrategyFunction ).withIndex( index )
				.withIndexProperty( index.name(), "directory.type", "local-filesystem" )
				.withIndexProperty( index.name(), "directory.root", temporaryFolder.toAbsolutePath().toString() )
				.withIndexProperty( index.name(), "shards." + shardIds.get( 2 ) + ".directory.root",
						anotherTemporaryFolder.toAbsolutePath().toString() )
				.withIndexProperty( index.name(), "shards." + shardIds.get( 3 ) + ".directory.type", "local-heap" )
				.setup();

		assertThat( shard0Directory ).exists();
		assertThat( shard1Directory ).exists();
		assertThat( shard2Directory ).exists();

		LuceneIndexManagerImpl luceneIndexManager = index.unwrapForTests( LuceneIndexManagerImpl.class );
		assertThat( luceneIndexManager.getShardsForTests() )
				.element( 0 )
				.extracting( Shard::indexAccessorForTests )
				.extracting( IndexAccessorImpl::getDirectoryForTests )
				.isInstanceOf( FSDirectory.class );
		assertThat( luceneIndexManager.getShardsForTests() )
				.element( 1 )
				.extracting( Shard::indexAccessorForTests )
				.extracting( IndexAccessorImpl::getDirectoryForTests )
				.isInstanceOf( FSDirectory.class );
		assertThat( luceneIndexManager.getShardsForTests() )
				.element( 2 )
				.extracting( Shard::indexAccessorForTests )
				.extracting( IndexAccessorImpl::getDirectoryForTests )
				.isInstanceOf( FSDirectory.class );
		assertThat( luceneIndexManager.getShardsForTests() )
				.element( 3 )
				.extracting( Shard::indexAccessorForTests )
				.extracting( IndexAccessorImpl::getDirectoryForTests )
				.isInstanceOf( ByteBuffersDirectory.class );
	}

}
