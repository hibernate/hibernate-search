/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration.norms;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.cfg.Configuration;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Norms;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.backend.LeakingLuceneBackend;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test storing and omitting index time norms
 *
 * @author Hardy Ferentschik
 */
public class StoreNormsTest extends SearchTestBase {

	@Test
	public void testStoreAndOmitNorms() throws Exception {
		Session session = openSession();
		FullTextSession fullTextSession = Search.getFullTextSession( session );
		Transaction tx = fullTextSession.beginTransaction();
		NormsTestEntity test = new NormsTestEntity();
		test.setWithNormsImplicit( "hello" );
		test.setWithNormsExplicit( "world" );
		test.setWithoutNorms( "how are you?" );
		fullTextSession.save( test );
		tx.commit();

		List<LuceneWork> processedQueue = LeakingLuceneBackend.getLastProcessedQueue();
		assertTrue( processedQueue.size() == 1 );
		AddLuceneWork addLuceneWork = (AddLuceneWork) processedQueue.get( 0 );
		Document doc = addLuceneWork.getDocument();

		IndexableField implicitNormField = doc.getField( "withNormsImplicit" );
		assertFalse( "norms should be stored for this field", implicitNormField.fieldType().omitNorms() );

		IndexableField explicitNormField = doc.getField( "withNormsExplicit" );
		assertFalse( "norms should be stored for this field", explicitNormField.fieldType().omitNorms() );

		IndexableField withoutNormField = doc.getField( "withoutNorms" );
		assertTrue( "norms should not be stored for this field", withoutNormField.fieldType().omitNorms() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { NormsTestEntity.class };
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "hibernate.search.default.worker.backend", LeakingLuceneBackend.class.getName() );
	}

	@Entity
	@Indexed(index = "test")
	public class NormsTestEntity {
		@Id
		@GeneratedValue
		private int id;

		@Field(store = Store.YES)
		private String withNormsImplicit;

		@Field(norms = Norms.YES, store = Store.YES)
		private String withNormsExplicit;

		@Field(norms = Norms.NO, store = Store.YES)
		private String withoutNorms;

		public String getWithNormsImplicit() {
			return withNormsImplicit;
		}

		public void setWithNormsImplicit(String withNormsImplicit) {
			this.withNormsImplicit = withNormsImplicit;
		}

		public String getWithNormsExplicit() {
			return withNormsExplicit;
		}

		public void setWithNormsExplicit(String withNormsExplicit) {
			this.withNormsExplicit = withNormsExplicit;
		}

		public String getWithoutNorms() {
			return withoutNorms;
		}

		public void setWithoutNorms(String withoutNorms) {
			this.withoutNorms = withoutNorms;
		}
	}
}
