/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.envers;

import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.hibernate.Transaction;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.PortedToSearch6;

import org.hibernate.testing.SkipForDialect;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit test covering proper behavior and integration between Hibernate Search and Envers.
 * As well as it verifies that the index is in sync with the latest transaction state.
 *
 * @author Davide Di Somma <davide.disomma@gmail.com>
 */
@SkipForDialect(jiraKey = "HSEARCH-1943", value = PostgreSQLDialect.class)
@Category(PortedToSearch6.class)
public class SearchAndEnversIntegrationTest extends SearchTestBase {

	private Person harryPotter;
	private Person hermioneGranger;
	private Address privetDrive;
	private Address grimmauldPlace;

	/**
	 * This test case aims to verify that insertion, updating and deleting operations work correctly
	 * for both Hibernate Search and Hibernate Envers
	 */
	@TestForIssue(jiraKey = "HSEARCH-1293")
	@Test
	public void testHibernateSearchAndEnversIntegration() {
		atRevision1();
		atRevision2();
		atRevision3();
		atRevision4();
	}

	/**
	 * It verifies that insertion operation works correctly
	 */
	private void atRevision1() {
		// Objects creation
		privetDrive = new Address( "Privet Drive", 121 );
		privetDrive.setFlatNumber( 2 );
		harryPotter = new Person( "Harry", "Potter", privetDrive );
		grimmauldPlace = new Address( "Grimmauld Place", 12 );
		hermioneGranger = new Person( "Hermione", "Granger", grimmauldPlace );

		{
			FullTextSession session = Search.getFullTextSession( openSession() );
			try {
				Transaction tx = session.beginTransaction();
				session.save( privetDrive );
				session.save( grimmauldPlace );
				session.save( harryPotter );
				session.save( hermioneGranger );
				tx.commit();
			}
			finally {
				session.close();
			}
		}

		{
			FullTextSession session = Search.getFullTextSession( openSession() );
			try {
				Transaction tx = session.beginTransaction();

				//Let's assert that Hibernate Envers has audited correctly
				AuditReader auditReader = AuditReaderFactory.get( session );
				assertEquals( 1, findLastRevisionForEntity( auditReader, Person.class ) );
				assertEquals( 1, findLastRevisionForEntity( auditReader, Address.class ) );
				assertEquals( 2, howManyEntitiesChangedAtRevisionNumber( auditReader, Person.class, 1 ) );
				assertEquals( 2, howManyEntitiesChangedAtRevisionNumber( auditReader, Address.class, 1 ) );
				assertEquals( 2, howManyAuditedObjectsSoFar( auditReader, Person.class ) );
				assertEquals( 2, howManyAuditedObjectsSoFar( auditReader, Address.class ) );
				//Let's compares that entities from Hibernate Search and last revision entities from Hibernate Envers are equals
				Person hermioneFromHibSearch = findPersonFromIndexBySurname( session, "Granger" );
				Person hermioneAtRevision1 = findPersonFromAuditBySurname( auditReader, "Granger" );
				assertEquals( hermioneFromHibSearch, hermioneAtRevision1 );
				Person harryFromHibSearch = findPersonFromIndexBySurname( session, "Potter" );
				Person harryAtRevision1 = findPersonFromAuditBySurname( auditReader, "Potter" );
				assertEquals( harryFromHibSearch, harryAtRevision1 );

				tx.commit();
			}
			finally {
				session.close();
			}
		}
	}

	/**
	 * It verifies that updating operation on Address entity works correctly
	 */
	private void atRevision2() {
		// Changing the address's house number and flat number
		{
			FullTextSession session = Search.getFullTextSession( openSession() );
			try {
				Transaction tx = session.beginTransaction();

				privetDrive = (Address) session.merge( privetDrive );
				privetDrive.setHouseNumber( 5 );
				privetDrive.setFlatNumber( null );

				tx.commit();
			}
			finally {
				session.close();
			}
		}

		{
			FullTextSession session = Search.getFullTextSession( openSession() );
			try {
				Transaction tx = session.beginTransaction();
				AuditReader auditReader = AuditReaderFactory.get( session );

				//Let's assert that Hibernate Envers has audited everything correctly
				assertEquals( 1, findLastRevisionForEntity( auditReader, Person.class ) );
				assertEquals( 2, findLastRevisionForEntity( auditReader, Address.class ) );
				assertEquals( 0, howManyEntitiesChangedAtRevisionNumber( auditReader, Person.class, 2 ) );
				assertEquals( 1, howManyEntitiesChangedAtRevisionNumber( auditReader, Address.class, 2 ) );
				assertEquals( 2, howManyAuditedObjectsSoFar( auditReader, Person.class ) );
				assertEquals( 3, howManyAuditedObjectsSoFar( auditReader, Address.class ) );
				@SuppressWarnings("unchecked")
				List<Address> houseNumberAddressChangedAtRevision2 = auditReader.createQuery()
						.forEntitiesModifiedAtRevision( Address.class, 2 )
						.add( AuditEntity.property( "houseNumber" ).hasChanged() )
						.add( AuditEntity.property( "flatNumber" ).hasChanged() )
						.add( AuditEntity.property( "streetName" ).hasNotChanged() ).getResultList();
				assertEquals( 1, houseNumberAddressChangedAtRevision2.size() );

				//Let's assert that Hibernate Search has indexed everything correctly
				List<Person> peopleLivingInPrivetDriveFromHibSearch = findPeopleFromIndexByStreetName( session, "privet" );
				assertEquals( 1, peopleLivingInPrivetDriveFromHibSearch.size() );
				//Let's compare that entities from Hibernate Search and last revision entities from Hibernate Envers are equals
				Person harryFromHibSearch = peopleLivingInPrivetDriveFromHibSearch.get( 0 );
				Person harryAtRevision2 = findPersonFromAuditBySurname( auditReader, "Potter" );
				assertEquals( harryFromHibSearch, harryAtRevision2 );

				tx.commit();
			}
			finally {
				session.close();
			}
		}
	}

	/**
	 * It verifies that updating operation on Person entity works correctly
	 */
	private void atRevision3() {
		// Moving Hermione to Harry
		{
			FullTextSession session = Search.getFullTextSession( openSession() );
			try {
				Transaction tx = session.beginTransaction();

				hermioneGranger = (Person) session.merge( hermioneGranger );
				hermioneGranger.setAddress( privetDrive );

				tx.commit();
			}
			finally {
				session.close();
			}
		}

		{
			FullTextSession session = Search.getFullTextSession( openSession() );
			try {
				Transaction tx = session.beginTransaction();
				AuditReader auditReader = AuditReaderFactory.get( session );

				//Let's assert that Hibernate Envers has audited everything correctly
				@SuppressWarnings("unchecked")
				List<Person> peopleWhoHasMovedHouseAtRevision3 = auditReader.createQuery()
						.forEntitiesModifiedAtRevision( Person.class, 3 ).add( AuditEntity.property( "address" ).hasChanged() )
						.getResultList();
				assertEquals( 1, peopleWhoHasMovedHouseAtRevision3.size() );
				assertEquals( 3, findLastRevisionForEntity( auditReader, Person.class ) );
				assertEquals( 3, findLastRevisionForEntity( auditReader, Address.class ) );
				assertEquals( 1, howManyEntitiesChangedAtRevisionNumber( auditReader, Person.class, 3 ) );
				assertEquals( 2, howManyEntitiesChangedAtRevisionNumber( auditReader, Address.class, 3 ) );
				assertEquals( 3, howManyAuditedObjectsSoFar( auditReader, Person.class ) );
				assertEquals( 5, howManyAuditedObjectsSoFar( auditReader, Address.class ) );
				//Let's assert that Hibernate Search has indexed everything correctly
				List<Person> peopleLivingInPrivetDriveFromHibSearch = findPeopleFromIndexByStreetName( session, "privet" );
				assertEquals( 2, peopleLivingInPrivetDriveFromHibSearch.size() );

				tx.commit();
			}
			finally {
				session.close();
			}
		}
	}

	/**
	 * It verifies that deleting operation on Person entity works correctly
	 */
	private void atRevision4() {
		// Now let's clean up everything
		{
			FullTextSession session = Search.getFullTextSession( openSession() );
			try {
				Transaction tx = session.beginTransaction();
				session.delete( harryPotter );
				session.delete( hermioneGranger );
				session.delete( grimmauldPlace );
				session.delete( privetDrive );
				tx.commit();
			}
			finally {
				session.close();
			}
		}

		{
			FullTextSession session = Search.getFullTextSession( openSession() );
			try {
				Transaction tx = session.beginTransaction();
				AuditReader auditReader = AuditReaderFactory.get( session );

				//Let's assert that Hibernate Envers has audited everything correctly
				assertEquals( 4, findLastRevisionForEntity( auditReader, Person.class ) );
				assertEquals( 4, findLastRevisionForEntity( auditReader, Address.class ) );
				assertEquals( 2, howManyEntitiesChangedAtRevisionNumber( auditReader, Person.class, 4 ) );
				assertEquals( 2, howManyEntitiesChangedAtRevisionNumber( auditReader, Address.class, 4 ) );
				assertEquals( 7, howManyAuditedObjectsSoFar( auditReader, Address.class ) );
				assertEquals( 5, howManyAuditedObjectsSoFar( auditReader, Person.class ) );
				//Let's assert that Hibernate Search has indexed everything correctly
				assertNull( findPersonFromIndexBySurname( session, "Potter" ) );
				assertNull( findPersonFromIndexBySurname( session, "Granger" ) );
				assertEquals( 0, findPeopleFromIndexByStreetName( session, "privet" ).size() );
				assertEquals( 0, findPeopleFromIndexByStreetName( session, "Guillaume" ).size() );

				tx.commit();
			}
			finally {
				session.close();
			}
		}
	}

	/**
	 * It returns how many entities are modified for a specific class and number revision.
	 */
	private int howManyEntitiesChangedAtRevisionNumber(AuditReader auditReader, Class<?> clazz, Number revision) {
		return ( (Long) auditReader.createQuery().forEntitiesModifiedAtRevision( clazz, revision )
				.addProjection( AuditEntity.id().count() ).getSingleResult() ).intValue();
	}

	/**
	 * It returns how many audited objects are there globally for a specific class.
	 */
	private int howManyAuditedObjectsSoFar(AuditReader auditReader, Class<?> clazz) {
		return auditReader.createQuery().forRevisionsOfEntity( clazz, true, true ).getResultList().size();
	}

	/**
	 * It returns the last revision for a specific class.
	 */
	private Number findLastRevisionForEntity(AuditReader auditReader, Class<?> clazz) {
		return (Number) auditReader.createQuery().forRevisionsOfEntity( clazz, false, true )
				.addProjection( AuditEntity.revisionNumber().max() ).getSingleResult();
	}

	private Person findPersonFromAuditBySurname(AuditReader auditReader, String value) {
		return (Person) auditReader.createQuery().forEntitiesAtRevision( Person.class, 1 )
				.add( AuditEntity.property( "surname" ).eq( value ) ).getSingleResult();
	}

	@SuppressWarnings("unchecked")
	private List<Person> findPeopleFromIndex(FullTextSession session, String term, String value) {
		QueryBuilder qb = session.getSearchFactory().buildQueryBuilder().forEntity( Person.class ).get();
		Query luceneQuery = createLuceneQuery( term, value );
		return session.createFullTextQuery( luceneQuery, Person.class )
				.setSort( qb.sort().byField( "surname" ).createSort() ).list();
	}

	private Person findPersonFromIndexBySurname(FullTextSession session, String surname) {
		List<Person> people = findPeopleFromIndex( session, "surname", surname );
		if ( people.isEmpty() ) {
			return null;
		}
		else if ( people.size() > 1 ) {
			throw new RuntimeException( "I've found too many people!!!" );
		}
		return people.get( 0 );
	}

	private List<Person> findPeopleFromIndexByStreetName(FullTextSession session, String streetName) {
		return findPeopleFromIndex( session, "address.streetName", streetName );
	}

	private Query createLuceneQuery(String term, String value) {
		return new TermQuery( new Term( term, value ) );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Person.class, Address.class };
	}
}
