/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.indexmanager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.Lock;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * @author gustavonalle
 */
@TestForIssue(jiraKey = "HSEARCH-2012")
@Category(SkipOnElasticsearch.class) // DirectoryBasedIndexManager is specific to Lucene
public class DirectoryBasedIndexManagerTest {

	@Rule
	public final SearchFactoryHolder sfh = new SearchFactoryHolder( Entity.class );

	private final SearchITHelper helper = new SearchITHelper( sfh );

	@Test
	public void testUnlockIndexWriter() throws Exception {
		ExtendedSearchIntegrator searchIntegrator = sfh.getSearchFactory();
		IndexManager indexManager = searchIntegrator.getIndexBindings().get( Entity.class )
				.getIndexManagerSelector().all().iterator().next();
		helper.add( new Entity(), 1 );
		assertTrue( isIndexWriterLocked( indexManager ) );

		indexManager.flushAndReleaseResources();
		assertFalse( isIndexWriterLocked( indexManager ) );

		helper.add( new Entity(), 2 );
		assertTrue( isIndexWriterLocked( indexManager ) );
	}

	private boolean isIndexWriterLocked(IndexManager indexManager) {
		Directory directory = ( (DirectoryBasedIndexManager) indexManager ).getDirectoryProvider().getDirectory();
		Lock lock = null;
		try {
			lock = directory.obtainLock( IndexWriter.WRITE_LOCK_NAME );
		}
		catch (IOException e) {
			return true;
		}
		finally {
			if ( lock != null ) {
				try {
					lock.close();
				}
				catch (Exception ignored) {
				}
			}

		}
		return false;
	}

}

@Indexed
class Entity {
	@DocumentId
	int id;
}
