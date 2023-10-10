/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.envers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

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
import org.hibernate.search.testsupport.junit.Tags;

import org.hibernate.testing.orm.junit.SkipForDialect;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/**
 * Unit test covering proper behavior and integration between Hibernate Search and Envers.
 * As well as it verifies that the index is in sync with the latest transaction state.
 *
 * @author Davide Di Somma <davide.disomma@gmail.com>
 */
@SkipForDialect(reason = "HSEARCH-1943", dialectClass = PostgreSQLDialect.class)
@Tag(Tags.PORTED_TO_SEARCH_6)
class SearchAndEnversIntegrationTest extends SearchTestBase {

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
	void testHibernateSearchAndEnversIntegration() {
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
				assertThat( findLastRevisionForEntity( auditReader, Person.class ) ).isEqualTo( 1 );
				assertThat( findLastRevisionForEntity( auditReader, Address.class ) ).isEqualTo( 1 );
				assertThat( howManyEntitiesChangedAtRevisionNumber( auditReader, Person.class, 1 ) ).isEqualTo( 2 );
				assertThat( howManyEntitiesChangedAtRevisionNumber( auditReader, Address.class, 1 ) ).isEqualTo( 2 );
				assertThat( howManyAuditedObjectsSoFar( auditReader, Person.class ) ).isEqualTo( 2 );
				assertThat( howManyAuditedObjectsSoFar( auditReader, Address.class ) ).isEqualTo( 2 );
				//Let's compares that entities from Hibernate Search and last revision entities from Hibernate Envers are equals
				Person hermioneFromHibSearch = findPersonFromIndexBySurname( session, "Granger" );
				Person hermioneAtRevision1 = findPersonFromAuditBySurname( auditReader, "Granger" );
				assertThat( hermioneAtRevision1 ).isEqualTo( hermioneFromHibSearch );
				Person harryFromHibSearch = findPersonFromIndexBySurname( session, "Potter" );
				Person harryAtRevision1 = findPersonFromAuditBySurname( auditReader, "Potter" );
				assertThat( harryAtRevision1 ).isEqualTo( harryFromHibSearch );

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
				assertThat( findLastRevisionForEntity( auditReader, Person.class ) ).isEqualTo( 1 );
				assertThat( findLastRevisionForEntity( auditReader, Address.class ) ).isEqualTo( 2 );
				assertThat( howManyEntitiesChangedAtRevisionNumber( auditReader, Person.class, 2 ) ).isZero();
				assertThat( howManyEntitiesChangedAtRevisionNumber( auditReader, Address.class, 2 ) ).isEqualTo( 1 );
				assertThat( howManyAuditedObjectsSoFar( auditReader, Person.class ) ).isEqualTo( 2 );
				assertThat( howManyAuditedObjectsSoFar( auditReader, Address.class ) ).isEqualTo( 3 );
				@SuppressWarnings("unchecked")
				List<Address> houseNumberAddressChangedAtRevision2 = auditReader.createQuery()
						.forEntitiesModifiedAtRevision( Address.class, 2 )
						.add( AuditEntity.property( "houseNumber" ).hasChanged() )
						.add( AuditEntity.property( "flatNumber" ).hasChanged() )
						.add( AuditEntity.property( "streetName" ).hasNotChanged() ).getResultList();
				assertThat( houseNumberAddressChangedAtRevision2 ).hasSize( 1 );

				//Let's assert that Hibernate Search has indexed everything correctly
				List<Person> peopleLivingInPrivetDriveFromHibSearch = findPeopleFromIndexByStreetName( session, "privet" );
				assertThat( peopleLivingInPrivetDriveFromHibSearch ).hasSize( 1 );
				//Let's compare that entities from Hibernate Search and last revision entities from Hibernate Envers are equals
				Person harryFromHibSearch = peopleLivingInPrivetDriveFromHibSearch.get( 0 );
				Person harryAtRevision2 = findPersonFromAuditBySurname( auditReader, "Potter" );
				assertThat( harryAtRevision2 ).isEqualTo( harryFromHibSearch );

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
				assertThat( peopleWhoHasMovedHouseAtRevision3.size() ).isEqualTo( 1 );
				assertThat( findLastRevisionForEntity( auditReader, Person.class ) ).isEqualTo( 3 );
				assertThat( findLastRevisionForEntity( auditReader, Address.class ) ).isEqualTo( 3 );
				assertThat( howManyEntitiesChangedAtRevisionNumber( auditReader, Person.class, 3 ) ).isEqualTo( 1 );
				assertThat( howManyEntitiesChangedAtRevisionNumber( auditReader, Address.class, 3 ) ).isEqualTo( 2 );
				assertThat( howManyAuditedObjectsSoFar( auditReader, Person.class ) ).isEqualTo( 3 );
				assertThat( howManyAuditedObjectsSoFar( auditReader, Address.class ) ).isEqualTo( 5 );
				//Let's assert that Hibernate Search has indexed everything correctly
				List<Person> peopleLivingInPrivetDriveFromHibSearch = findPeopleFromIndexByStreetName( session, "privet" );
				assertThat( peopleLivingInPrivetDriveFromHibSearch ).hasSize( 2 );

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
				assertThat( findLastRevisionForEntity( auditReader, Person.class ) ).isEqualTo( 4 );
				assertThat( findLastRevisionForEntity( auditReader, Address.class ) ).isEqualTo( 4 );
				assertThat( howManyEntitiesChangedAtRevisionNumber( auditReader, Person.class, 4 ) ).isEqualTo( 2 );
				assertThat( howManyEntitiesChangedAtRevisionNumber( auditReader, Address.class, 4 ) ).isEqualTo( 2 );
				assertThat( howManyAuditedObjectsSoFar( auditReader, Address.class ) ).isEqualTo( 7 );
				assertThat( howManyAuditedObjectsSoFar( auditReader, Person.class ) ).isEqualTo( 5 );
				//Let's assert that Hibernate Search has indexed everything correctly
				assertThat( findPersonFromIndexBySurname( session, "Potter" ) ).isNull();
				assertThat( findPersonFromIndexBySurname( session, "Granger" ) ).isNull();
				assertThat( findPeopleFromIndexByStreetName( session, "privet" ) ).isEmpty();
				assertThat( findPeopleFromIndexByStreetName( session, "Guillaume" ) ).isEmpty();

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
