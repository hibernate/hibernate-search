/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.reader.nrtreaders;

import java.util.List;
import java.util.Map;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.hibernate.Session;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.indexes.impl.NRTIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.AlternateDocument;
import org.hibernate.search.test.Document;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.errorhandling.MockErrorHandler;
import org.hibernate.search.hcore.util.impl.ContextHelper;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Strongly inspired to RamDirectoryTest, verifies the searchability of unflushed
 * modifications on an NRT IndexManager.
 * Implicitly verifies that the NRTIndexManager is setup as configured.
 *
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class BasicNRTFunctionalityTest extends SearchTestBase {

	/**
	 * Verify it's safe to not skip deletes even when a new entity
	 * is stored reusing a stale documentId.
	 */
	@Test
	public void testEntityResurrection() {
		final Long id = 5l;
		Session session = getSessionFactory().openSession();
		session.getTransaction().begin();

		AlternateDocument docOnInfinispan = new AlternateDocument( id, "On Infinispan", "a book about Infinispan", "content" );
		session.persist( docOnInfinispan );
		session.getTransaction().commit();
		session.clear();

		session.getTransaction().begin();
		FullTextSession fullTextSession = Search.getFullTextSession( session );
		QueryBuilder queryBuilder = fullTextSession.getSearchFactory().buildQueryBuilder().forEntity( AlternateDocument.class ).get();
		Query luceneQuery = queryBuilder.keyword().onField( "Abstract" ).matching( "Infinispan" ).createQuery();
		List list = fullTextSession.createFullTextQuery( luceneQuery ).list();
		Assert.assertEquals( 1, list.size() );
		session.getTransaction().commit();
		session.clear();

		session.getTransaction().begin();
		Object loadedDocument = session.load( AlternateDocument.class, id );
		session.delete( loadedDocument );
		session.getTransaction().commit();
		session.clear();

		session.getTransaction().begin();
		list = fullTextSession.createFullTextQuery( luceneQuery ).list();
		Assert.assertEquals( 0, list.size() );

		AlternateDocument docOnHibernate = new AlternateDocument( id, "On Hibernate", "a book about Hibernate", "content" );
		session.persist( docOnHibernate );
		session.getTransaction().commit();

		session.getTransaction().begin();
		list = fullTextSession.createFullTextQuery( luceneQuery ).list();
		Assert.assertEquals( 0, list.size() );

		session.close();
	}

	@Test
	public void testMultipleEntitiesPerIndex() throws Exception {
		ExtendedSearchIntegrator integrator = ContextHelper.getSearchIntegratorBySF( getSessionFactory() );
		IndexManager documentsIndexManager = integrator.getIndexManagerHolder().getIndexManager( "Documents" );
		Assert.assertNotNull( documentsIndexManager );
		Assert.assertTrue( documentsIndexManager.getClass().equals( org.hibernate.search.indexes.impl.NRTIndexManager.class ) );
		NRTIndexManager indexManager = (NRTIndexManager) documentsIndexManager;

		Session s = getSessionFactory().openSession();
		s.getTransaction().begin();
		Document document =
				new Document( "Hibernate in Action", "Object/relational mapping with Hibernate", "blah blah blah" );
		s.persist( document );
		s.flush();
		s.persist(
				new AlternateDocument(
						document.getId(),
						"Hibernate in Action",
						"Object/relational mapping with Hibernate",
						"blah blah blah"
				)
		);
		s.getTransaction().commit();
		s.close();

		assertEquals( 0, getDocumentNbrFromFilesystem( indexManager ) );
		assertEquals( 2, getDocumentNbrFromReaderProvider( indexManager ) );

		s = getSessionFactory().openSession();
		s.getTransaction().begin();
		TermQuery q = new TermQuery( new Term( "alt_title", "hibernate" ) );
		assertEquals(
				"does not properly filter", 0,
				Search.getFullTextSession( s ).createFullTextQuery( q, Document.class ).list().size()
		);
		assertEquals(
				"does not properly filter", 1,
				Search.getFullTextSession( s )
						.createFullTextQuery( q, Document.class, AlternateDocument.class )
						.list().size()
		);
		s.delete( s.get( AlternateDocument.class, document.getId() ) );
		s.getTransaction().commit();
		s.close();

		s = getSessionFactory().openSession();
		s.getTransaction().begin();

		assertEquals( 0, getDocumentNbrFromFilesystem( indexManager ) ); //filesystem only sees changes from a fully closed IW
		assertEquals( 1, getDocumentNbrFromQuery( s ) ); //Hibernate Search has to return the right answer
		assertEquals( 1, getDocumentNbrFromReaderProvider( indexManager ) ); //In-memory buffers might fail to see deletes

		s.delete( s.createCriteria( Document.class ).uniqueResult() );
		s.getTransaction().commit();
		s.close();

		s = getSessionFactory().openSession();
		s.getTransaction().begin();

		assertEquals( 0, getDocumentNbrFromFilesystem( indexManager ) );
		assertEquals( 0, getDocumentNbrFromQuery( s ) );
		assertEquals( 0, getDocumentNbrFromReaderProvider( indexManager ) );

		s.getTransaction().commit();
		s.close();

		ErrorHandler errorHandler = integrator.getErrorHandler();
		Assert.assertTrue( errorHandler instanceof MockErrorHandler );
		MockErrorHandler mockErrorHandler = (MockErrorHandler)errorHandler;
		Assert.assertNull( "Errors detected in the backend!", mockErrorHandler.getLastException() );
	}

	private int getDocumentNbrFromQuery(Session currentSession) {
		MatchAllDocsQuery luceneQuery = new MatchAllDocsQuery();
		FullTextQuery fullTextQuery = Search.getFullTextSession( currentSession ).createFullTextQuery( luceneQuery, Document.class );
		return fullTextQuery.list().size();
	}

	private int getDocumentNbrFromReaderProvider(NRTIndexManager indexManager) {
		IndexReader reader = indexManager.getReaderProvider().openIndexReader();
		try {
			return reader.numDocs();
		}
		finally {
			indexManager.getReaderProvider().closeIndexReader( reader );
		}
	}

	private int getDocumentNbrFromFilesystem(NRTIndexManager documentsIndexManager) throws Exception {
		IndexReader reader = DirectoryReader.open( documentsIndexManager.getDirectoryProvider().getDirectory() );
		try {
			return reader.numDocs();
		}
		finally {
			reader.close();
		}
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Document.class,
				AlternateDocument.class
		};
	}

	@Override
	public void configure(Map<String,Object> cfg) {
		cfg.put( "hibernate.search.default.indexmanager", "near-real-time" );
		cfg.put( Environment.ERROR_HANDLER, MockErrorHandler.class.getName() );
	}

}
