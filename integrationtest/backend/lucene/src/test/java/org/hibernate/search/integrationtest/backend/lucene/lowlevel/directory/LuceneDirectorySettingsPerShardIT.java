/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.lowlevel.directory;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;

import org.hibernate.search.backend.lucene.index.impl.LuceneIndexManagerImpl;
import org.hibernate.search.backend.lucene.index.impl.Shard;
import org.hibernate.search.backend.lucene.lowlevel.index.impl.IndexAccessorImpl;
import org.hibernate.search.integrationtest.backend.lucene.sharding.AbstractSettingsPerShardIT;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Test;

import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.FSDirectory;

@TestForIssue(jiraKey = "HSEARCH-3636")
public class LuceneDirectorySettingsPerShardIT extends AbstractSettingsPerShardIT {

	public LuceneDirectorySettingsPerShardIT(String ignoredLabel, SearchSetupHelper setupHelper, List<String> shardIds) {
		super( ignoredLabel, setupHelper, shardIds );
	}

	@Test
	public void test() {
		Path root1Directory = temporaryFolder.getRoot().toPath();
		Path shard0Directory = root1Directory.resolve( index.name() ).resolve( shardIds.get( 0 ) );
		Path shard1Directory = root1Directory.resolve( index.name() ).resolve( shardIds.get( 1 ) );
		Path root2Directory = temporaryFolder.getRoot().toPath();
		Path shard2Directory = root2Directory.resolve( index.name() ).resolve( shardIds.get( 2 ) );

		assertThat( shard0Directory ).doesNotExist();
		assertThat( shard1Directory ).doesNotExist();
		assertThat( shard2Directory ).doesNotExist();

		setupHelper.start().withIndex( index )
				.withIndexProperty( index.name(), "directory.type", "local-filesystem" )
				.withIndexProperty( index.name(), "directory.root", root1Directory.toString() )
				.withIndexProperty( index.name(), "shards." + shardIds.get( 2 ) + ".directory.root", root2Directory.toString() )
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
