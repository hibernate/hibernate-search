/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.indexmanager;

import java.io.IOException;
import java.io.Serializable;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.Lock;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.spi.SearchIntegratorBuilder;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.indexmanager.RamIndexManager;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * @author gustavonalle
 */
@TestForIssue(jiraKey = "HSEARCH-2012")
public class IndexManagerTest {

	@Test
	public void testUnlockIndexWriter() throws Exception {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest()
				.addProperty( "hibernate.search.default.indexmanager", RamIndexManager.class.getName() )
				.addClass( Entity.class );
		SearchIntegrator searchIntegrator = new SearchIntegratorBuilder().configuration( cfg ).buildSearchIntegrator();
		IndexManager indexManager = searchIntegrator.getIndexBinding( Entity.class ).getIndexManagers()[0];
		addEntity( searchIntegrator, 1 );
		assertTrue( isIndexWriterLocked( indexManager ) );

		indexManager.closeIndexWriter();
		assertFalse( isIndexWriterLocked( indexManager ) );

		addEntity( searchIntegrator, 2 );
		assertTrue( isIndexWriterLocked( indexManager ) );
	}

	private boolean isIndexWriterLocked(IndexManager indexManager) {
		Directory directory = ( (RamIndexManager) indexManager ).getDirectoryProvider().getDirectory();
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

	private void addEntity(SearchIntegrator searchIntegrator, Serializable id) {
		Entity entity = new Entity();
		Work work = new Work( entity, id, WorkType.ADD, false );
		TransactionContextForTest tc = new TransactionContextForTest();
		searchIntegrator.getWorker().performWork( work, tc );
		tc.end();
	}

}

@Indexed
class Entity {
	@DocumentId
	int id;
}
