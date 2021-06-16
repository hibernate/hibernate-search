/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.FlushEventListener;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.backend.impl.PerTransactionWorker;
import org.hibernate.search.event.impl.FullTextIndexEventListener;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.test.Document;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.util.impl.ConcurrentReferenceHashMap;

import org.junit.After;
import org.junit.Test;

@TestForIssue(jiraKey = "HSEARCH-4225")
public class NoTransactionTest extends SearchTestBase {

	@Test
	public void flush() throws Exception {
		long doc1Id;
		long doc2Id;

		try ( Session session = getSessionFactory().openSession() ) {
			Document doc1 = new Document(
					"Hibernate in Action", "Object/relational mapping with Hibernate", "blah blah blah" );
			session.persist( doc1 );
			Document doc2 = new Document( "Lucene in Action", "FullText search engine", "blah blah blah" );
			session.persist( doc2 );
			session.flush();
			doc1Id = doc1.getId();
			doc2Id = doc2.getId();
		}
		assertThat( countDocuments() ).isEqualTo( 2 );

		// Purge so that we can reindex
		purgeIndex();
		assertThat( countDocuments() ).isZero();

		// Reindex manually
		try ( Session session = getSessionFactory().openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			fullTextSession.setHibernateFlushMode( FlushMode.MANUAL );
			fullTextSession.setCacheMode( CacheMode.IGNORE );
			fullTextSession.index( session.load( Document.class, doc1Id ) );
			fullTextSession.index( session.load( Document.class, doc2Id ) );
			fullTextSession.flush();
		}
		assertThat( countDocuments() ).isEqualTo( 2 );
	}

	@Test
	public void flushToIndexes() throws Exception {
		long doc1Id;
		long doc2Id;

		try ( Session session = getSessionFactory().openSession() ) {
			Document doc1 = new Document(
					"Hibernate in Action", "Object/relational mapping with Hibernate", "blah blah blah" );
			session.persist( doc1 );
			Document doc2 = new Document( "Lucene in Action", "FullText search engine", "blah blah blah" );
			session.persist( doc2 );
			session.flush();
			doc1Id = doc1.getId();
			doc2Id = doc2.getId();
		}
		assertThat( countDocuments() ).isEqualTo( 2 );

		// Purge so that we can reindex
		purgeIndex();
		assertThat( countDocuments() ).isZero();

		// Reindex manually
		try ( Session session = getSessionFactory().openSession() ) {
			session.setHibernateFlushMode( FlushMode.MANUAL );
			session.setCacheMode( CacheMode.IGNORE );
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			fullTextSession.index( session.load( Document.class, doc1Id ) );
			fullTextSession.index( session.load( Document.class, doc2Id ) );
			fullTextSession.flushToIndexes();
		}
		assertThat( countDocuments() ).isEqualTo( 2 );
	}

	@After
	public void checkMemory() {
		System.gc();
		// Check there are no memory leaks
		assertThat( purgeStaleEntries( getPerTransactionWorker().synchronizationPerTransactionForTests() ) )
				.isEmpty();
		assertThat( purgeStaleEntries( getFullTextIndexEventListener().flushSynchForTests() ) )
				.isEmpty();
	}

	// Instances of ConcurrentReferenceHashMap only discover that weak references
	// are no longer valid when we actually access content, not when we access the size.
	// This forces the map to purge all entries whose weak reference has been GC'd so that the size is up to date.
	private <T extends Map<?, ?>> T purgeStaleEntries(T map) {
		( (ConcurrentReferenceHashMap<?, ?>) map ).purgeStaleEntries();
		return map;
	}

	private void purgeIndex() {
		try ( Session session = getSessionFactory().openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			Transaction tx = fullTextSession.beginTransaction();
			fullTextSession.purgeAll( Document.class );
			tx.commit();
		}
	}

	private PerTransactionWorker getPerTransactionWorker() {
		return (PerTransactionWorker) getSearchFactory().unwrap( SearchIntegrator.class ).getWorker();
	}

	private FullTextIndexEventListener getFullTextIndexEventListener() {
		for ( FlushEventListener listener : getSessionFactory().unwrap( SessionFactoryImplementor.class )
				.getServiceRegistry()
				.getService( EventListenerRegistry.class )
				.getEventListenerGroup( EventType.FLUSH )
				.listeners() ) {
			if ( listener instanceof FullTextIndexEventListener ) {
				return (FullTextIndexEventListener) listener;
			}
		}
		throw new IllegalStateException( "FullTextIndexEventListener not found?" );
	}

	private int countDocuments() {
		return getNumberOfDocumentsInIndex( Document.class );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Document.class };
	}

	@Override
	public void configure(Map<String, Object> cfg) {
		cfg.put( AvailableSettings.ALLOW_UPDATE_OUTSIDE_TRANSACTION, "true" );
	}

}
