/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.engine;

import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.test.Document;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.embedded.depth.PersonWithBrokenSocialSecurityNumber;
import org.hibernate.search.test.errorhandling.MockErrorHandler;
import org.hibernate.search.testsupport.backend.LeakingLocalBackend;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/**
 * When using hibernate.use_identifier_rollback=true special care must be applied during event processing to get a
 * reference to the identifiers of deleted entities. See HSEARCH-650.
 *
 * @author Sanne Grinovero
 * @since 3.3.1, 3.4.0
 */
public class UsingIdentifierRollbackTest extends SearchTestBase {

	@Test
	public void testEntityDeletionWithoutIdentifier() {
		SearchIntegrator integrator = getExtendedSearchIntegrator();
		MockErrorHandler errorHandler = (MockErrorHandler) integrator.getErrorHandler();

		Session s = getSessionFactory().openSession();
		s.getTransaction().begin();
		s.persist( new Document( "Hibernate in Action", "Object/relational mapping with Hibernate", "blah blah blah" ) );
		s.getTransaction().commit();
		s.close();

		s = getSessionFactory().openSession();
		s.getTransaction().begin();
		Document entity = s.get( Document.class, Long.valueOf( 1 ) );
		Assert.assertNotNull( entity );
		s.delete( entity );
		s.getTransaction().commit();
		s.close();
		Assert.assertNull( "unexpected exception detected", errorHandler.getLastException() );
	}

	@Test
	public void testRolledBackIdentifiersOnUnusualDocumentId() {
		SearchIntegrator integrator = getExtendedSearchIntegrator();
		MockErrorHandler errorHandler = (MockErrorHandler) integrator.getErrorHandler();

		Session s = getSessionFactory().openSession();
		s.getTransaction().begin();
		s.persist( new PersonWithBrokenSocialSecurityNumber( Long.valueOf( 2 ), "This guy is unaffected by identifier rollback" ) );
		s.getTransaction().commit();
		s.close();

		s = getSessionFactory().openSession();
		s.getTransaction().begin();
		PersonWithBrokenSocialSecurityNumber entity = s.get( PersonWithBrokenSocialSecurityNumber.class, Long.valueOf( 2 ) );
		Assert.assertNotNull( entity );
		s.delete( entity );
		s.getTransaction().commit();
		s.close();
		Assert.assertNull( "unexpected exception detected", errorHandler.getLastException() );
		List<LuceneWork> processedQueue = LeakingLocalBackend.getLastProcessedQueue();
		Assert.assertEquals( 1, processedQueue.size() );
		LuceneWork luceneWork = processedQueue.get( 0 );
		Assert.assertEquals( "100", luceneWork.getIdInString() );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Document.class, PersonWithBrokenSocialSecurityNumber.class };
	}

	@Override
	public void configure(Map<String,Object> cfg) {
		super.configure( cfg );
		cfg.put( "hibernate.use_identifier_rollback", "true" );
		cfg.put( Environment.ERROR_HANDLER, MockErrorHandler.class.getName() );
		cfg.put( "hibernate.search.default.worker.backend", LeakingLocalBackend.class.getName() );
	}

	@Override
	@After
	public void tearDown() throws Exception {
		super.tearDown();
		LeakingLocalBackend.reset();
	}

}
