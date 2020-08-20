/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.id;

import static org.junit.Assert.assertEquals;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.hibernate.search.testsupport.setup.CountingErrorHandler;
import org.junit.Rule;
import org.junit.Test;

/**
 * Verifies the MassIndexer can deal correctly with entities which have a composite id
 */
public class CompositedIdMassIndexingTest {

	@Rule
	public FullTextSessionBuilder ftsBuilder = new FullTextSessionBuilder()
			.addAnnotatedClass( RegistrationId.class )
			.addAnnotatedClass( StudentEntity.class )
			.setProperty( "hibernate.search.error_handler", CountingErrorHandler.class.getName() )
			.build();

	@Test
	public void testReindexingWithCompositeIds() throws InterruptedException {
		try ( FullTextSession fullTextSession = ftsBuilder.openFullTextSession() ) {
			storeTestData( fullTextSession );
		}
		try ( FullTextSession fullTextSession = ftsBuilder.openFullTextSession() ) {
			fullTextSession.createIndexer().startAndWait();
		}
		SearchFactory searchFactory = ftsBuilder.getSearchFactory();
		SearchIntegrator searchIntegrator = searchFactory.unwrap( SearchIntegrator.class );
		CountingErrorHandler errorHandler = (CountingErrorHandler) searchIntegrator.getErrorHandler();
		assertEquals( 0, errorHandler.getTotalCount() );
	}

	private void storeTestData(Session session) {
		Transaction tx = session.beginTransaction();
		RegistrationId firstId = new RegistrationId();
		firstId.setDepartment( "Software Engineering" );
		firstId.setStudentId( 1 );
		StudentEntity firstStudent = new StudentEntity();
		firstStudent.setRegid( firstId );
		firstStudent.setName( "I am noone" );
		session.save( firstStudent );
		tx.commit();
		session.clear();
	}

}
