/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.lowlevel.directory;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.search.backend.lucene.index.impl.LuceneIndexManagerImpl;
import org.hibernate.search.backend.lucene.index.impl.Shard;
import org.hibernate.search.backend.lucene.lowlevel.index.impl.IndexAccessorImpl;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.apache.lucene.store.ByteBuffersDirectory;

public class LuceneLocalHeapDirectoryIT extends AbstractBuiltInDirectoryIT {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3440")
	@PortedFromSearch5(original = "org.hibernate.search.test.directoryProvider.RamDirectoryTest.localHeap")
	public void test() {
		setup( c -> c );

		checkIndexingAndQuerying();

		LuceneIndexManagerImpl luceneIndexManager = index.unwrapForTests( LuceneIndexManagerImpl.class );
		assertThat( luceneIndexManager.getShardsForTests() )
				.extracting( Shard::getIndexAccessorForTests )
				.extracting( IndexAccessorImpl::getDirectoryForTests )
				.allSatisfy( directory -> assertThat( directory ).isInstanceOf( ByteBuffersDirectory.class ) );
	}

	@Override
	protected Object getDirectoryType() {
		return "local-heap";
	}

	@Override
	protected boolean isFSDirectory() {
		return false;
	}

	@Override
	protected String getDefaultLockClassName() {
		return SINGLE_INSTANCE_LOCK_FQN;
	}

}
