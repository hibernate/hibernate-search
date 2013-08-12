/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.engine;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.search.Environment;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.test.Document;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.util.LeakingLuceneBackend;
import org.hibernate.search.test.embedded.depth.PersonWithBrokenSocialSecurityNumber;
import org.hibernate.search.test.errorhandling.MockErrorHandler;
import org.junit.Assert;

/**
 * When using hibernate.use_identifier_rollback=true special care must be applied during event processing to get a
 * reference to the identifiers of deleted entities. See HSEARCH-650.
 *
 * @author Sanne Grinovero
 * @since 3.3.1, 3.4.0
 */
public class UsingIdentifierRollbackTest extends SearchTestCase {

	public void testEntityDeletionWithoutIdentifier() {
		SearchFactoryImplementor searchFactoryImpl = getSearchFactoryImpl();
		MockErrorHandler errorHandler = (MockErrorHandler) searchFactoryImpl.getErrorHandler();

		Session s = getSessionFactory().openSession();
		s.getTransaction().begin();
		s.persist( new Document( "Hibernate in Action", "Object/relational mapping with Hibernate", "blah blah blah" ) );
		s.getTransaction().commit();
		s.close();

		s = getSessionFactory().openSession();
		s.getTransaction().begin();
		Document entity = (Document) s.get( Document.class, Long.valueOf( 1 ) );
		Assert.assertNotNull( entity );
		s.delete( entity );
		s.getTransaction().commit();
		s.close();
		Assert.assertNull( "unexpected exception detected", errorHandler.getLastException() );
	}

	public void testRolledBackIdentifiersOnUnusualDocumentId() {
		SearchFactoryImplementor searchFactoryImpl = getSearchFactoryImpl();
		MockErrorHandler errorHandler = (MockErrorHandler) searchFactoryImpl.getErrorHandler();

		Session s = getSessionFactory().openSession();
		s.getTransaction().begin();
		s.persist( new PersonWithBrokenSocialSecurityNumber( Long.valueOf( 2 ), "This guy is unaffected by identifier rollback" ) );
		s.getTransaction().commit();
		s.close();

		s = getSessionFactory().openSession();
		s.getTransaction().begin();
		PersonWithBrokenSocialSecurityNumber entity = (PersonWithBrokenSocialSecurityNumber) s.get( PersonWithBrokenSocialSecurityNumber.class, Long.valueOf( 2 ) );
		Assert.assertNotNull( entity );
		s.delete( entity );
		s.getTransaction().commit();
		s.close();
		Assert.assertNull( "unexpected exception detected", errorHandler.getLastException() );
		List<LuceneWork> processedQueue = LeakingLuceneBackend.getLastProcessedQueue();
		Assert.assertEquals( 1, processedQueue.size() );
		LuceneWork luceneWork = processedQueue.get( 0 );
		Assert.assertEquals( "100", luceneWork.getIdInString() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Document.class, PersonWithBrokenSocialSecurityNumber.class };
	}

	@Override
	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "hibernate.use_identifier_rollback", "true" );
		cfg.setProperty( Environment.ERROR_HANDLER, MockErrorHandler.class.getName() );
		cfg.setProperty( "hibernate.search.default.worker.backend", LeakingLuceneBackend.class.getName() );
	}

	@Override
	public void tearDown() throws Exception {
		super.tearDown();
		LeakingLuceneBackend.reset();
	}

}
