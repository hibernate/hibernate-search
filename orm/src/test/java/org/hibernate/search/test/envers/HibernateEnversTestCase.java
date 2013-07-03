/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.test.envers;

import java.util.List;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.Transaction;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.TestConstants;
import org.hibernate.search.test.util.TestForIssue;

/**
 * Unit test covering proper behavior and integration between Hibernate Search and Envers.
 * As well as it verifies that the index is in sync with the latest transaction state.
 *
 * @author Davide Di Somma <davide.disomma@gmail.com>
 */
public class HibernateEnversTestCase extends SearchTestCase {

	@TestForIssue(jiraKey = "HSEARCH-1293")
	public void testUpdateIndexedEmbeddedCollectionWithNull() throws Exception {

		Address privetDriveAddress = new Address( "Privet Drive", 121 );
		privetDriveAddress.setFlatNumber( 2 );
		Person harryPotter = new Person( "Harry", "Potter", privetDriveAddress );
		Address grimmauldPlaceAddress = new Address( "Grimmauld Place", 12 );
		Person hermioneGranger = new Person( "Hermione", "Granger", grimmauldPlaceAddress );

		// Revision 1: objects creation
		FullTextSession session = Search.getFullTextSession( openSession() );
		Transaction tx = session.beginTransaction();
		session.save( privetDriveAddress );
		session.save( grimmauldPlaceAddress );
		session.save( harryPotter );
		session.save( hermioneGranger );
		tx.commit();
		session.close();

		// Revision 1: let's assert that everything got audited and indexed correctly
		session = Search.getFullTextSession( openSession() );
		tx = session.beginTransaction();

		AuditReader auditReader = AuditReaderFactory.get( session );
		assertEquals( 1, findLastRevisionForEntity( auditReader, Person.class ) );
		assertEquals( 1, findLastRevisionForEntity( auditReader, Address.class ) );
		assertEquals( 2, howManyChangesAtRevisionNumber( auditReader, Person.class, 1 ) );
		assertEquals( 2, howManyChangesAtRevisionNumber( auditReader, Address.class, 1 ) );
		assertEquals( 2, findAllAuditedObjects( auditReader, Person.class ).size() );
		assertEquals( 2, findAllAuditedObjects( auditReader, Address.class ).size() );
		Person hermioneFromHibSearch = findPersonFromIndexBySurname( session, "Granger" );
		Person hermioneAtRevision1 = findPersonFromAuditBySurname( auditReader, "Granger" );
		assertEquals( hermioneFromHibSearch, hermioneAtRevision1 );
		Person harryFromHibSearch = findPersonFromIndexBySurname( session, "Potter" );
		Person harryAtRevision1 = findPersonFromAuditBySurname( auditReader, "Potter" );
		assertEquals( harryFromHibSearch, harryAtRevision1 );

		tx.commit();
		session.close();

		// Revision 2: changing the address's house number and flat number
		session = Search.getFullTextSession( openSession() );
		tx = session.beginTransaction();

		privetDriveAddress = (Address) session.merge( privetDriveAddress );
		privetDriveAddress.setHouseNumber( 5 );
		privetDriveAddress.setFlatNumber( null );

		tx.commit();
		session.close();

		// Revision 2: assert that everything got audited and indexed correctly
		session = Search.getFullTextSession( openSession() );
		tx = session.beginTransaction();
		auditReader = AuditReaderFactory.get( session );

		List<Person> peopleLivingInPrivetDriveFromHibSearch = findPeopleFromIndexByStreetName( session, "Privet" );
		assertEquals( 1, peopleLivingInPrivetDriveFromHibSearch.size() );
		Person harryAtRevision2 = auditReader.find( Person.class, harryPotter.getId(), 2 );
		harryFromHibSearch = peopleLivingInPrivetDriveFromHibSearch.get( 0 );
		assertEquals( harryFromHibSearch, harryAtRevision2 );
		assertEquals( 1, findLastRevisionForEntity( auditReader, Person.class ) );
		assertEquals( 2, findLastRevisionForEntity( auditReader, Address.class ) );
		assertEquals( 0, howManyChangesAtRevisionNumber( auditReader, Person.class, 2 ) );
		assertEquals( 1, howManyChangesAtRevisionNumber( auditReader, Address.class, 2 ) );
		assertEquals( 2, findAllAuditedObjects( auditReader, Person.class ).size() );
		assertEquals( 3, findAllAuditedObjects( auditReader, Address.class ).size() );
		@SuppressWarnings("unchecked")
		List<Address> houseNumberAddressChangedAtRevision2 = auditReader.createQuery()
				.forEntitiesModifiedAtRevision( Address.class, 2 )
				.add( AuditEntity.property( "houseNumber" ).hasChanged() )
				.add( AuditEntity.property( "flatNumber" ).hasChanged() )
				.add( AuditEntity.property( "streetName" ).hasNotChanged() ).getResultList();
		assertEquals( 1, houseNumberAddressChangedAtRevision2.size() );
		Address privetDriveAtRevision2 = houseNumberAddressChangedAtRevision2.get( 0 );
		assertEquals( "Privet Drive", privetDriveAtRevision2.streetName );
		assertEquals( 5, privetDriveAtRevision2.houseNumber.intValue() );
		assertNull( privetDriveAtRevision2.flatNumber );
		assertEquals( 1, privetDriveAtRevision2.getPersons().size() );
		assertEquals( harryFromHibSearch.getAddress(), privetDriveAtRevision2 );

		tx.commit();
		session.close();

		// Revision 3: moving Hermione to Harry
		session = Search.getFullTextSession( openSession() );
		tx = session.beginTransaction();
		auditReader = AuditReaderFactory.get( session );

		hermioneGranger = (Person) session.merge( hermioneGranger );
		hermioneGranger.setAddress( privetDriveAddress );

		tx.commit();
		session.close();

		// Revision 3: assert that everything got audited and indexed correctly
		session = Search.getFullTextSession( openSession() );
		tx = session.beginTransaction();
		auditReader = AuditReaderFactory.get( session );

		peopleLivingInPrivetDriveFromHibSearch = findPeopleFromIndexByStreetName( session, "Privet" );
		assertEquals( 2, peopleLivingInPrivetDriveFromHibSearch.size() );
		@SuppressWarnings("unchecked")
		List<Person> peopleWhoHasMovedHouseAtRevision3 = auditReader.createQuery()
				.forEntitiesModifiedAtRevision( Person.class, 3 ).add( AuditEntity.property( "address" ).hasChanged() )
				.getResultList();
		assertEquals( 1, peopleWhoHasMovedHouseAtRevision3.size() );
		assertEquals( 3, findLastRevisionForEntity( auditReader, Person.class ) );
		assertEquals( 3, findLastRevisionForEntity( auditReader, Address.class ) );
		assertEquals( 1, howManyChangesAtRevisionNumber( auditReader, Person.class, 3 ) );
		assertEquals( 2, howManyChangesAtRevisionNumber( auditReader, Address.class, 3 ) );
		assertEquals( 3, findAllAuditedObjects( auditReader, Person.class ).size() );
		assertEquals( 5, findAllAuditedObjects( auditReader, Address.class ).size() );

		tx.commit();
		session.close();

		// Revision 4: Now let's clean up everything
		session = Search.getFullTextSession( openSession() );
		tx = session.beginTransaction();
		session.delete( harryPotter );
		session.delete( hermioneGranger );
		session.delete( grimmauldPlaceAddress );
		session.delete( privetDriveAddress );
		tx.commit();
		session.close();

		// Revision 4: assert that everything got audited and indexed correctly
		session = Search.getFullTextSession( openSession() );
		tx = session.beginTransaction();
		auditReader = AuditReaderFactory.get( session );

		assertNull( findPersonFromIndexBySurname( session, "Potter" ) );
		assertNull( findPersonFromIndexBySurname( session, "Granger" ) );
		assertEquals( 0, findPeopleFromIndexByStreetName( session, "Privet" ).size() );
		assertEquals( 0, findPeopleFromIndexByStreetName( session, "Guillaume" ).size() );
		assertEquals( 4, findLastRevisionForEntity( auditReader, Person.class ) );
		assertEquals( 4, findLastRevisionForEntity( auditReader, Address.class ) );
		assertEquals( 2, howManyChangesAtRevisionNumber( auditReader, Person.class, 4 ) );
		assertEquals( 2, howManyChangesAtRevisionNumber( auditReader, Address.class, 4 ) );
		assertEquals( 7, findAllAuditedObjects( auditReader, Address.class ).size() );
		assertEquals( 5, findAllAuditedObjects( auditReader, Person.class ).size() );

		tx.commit();
		session.close();
	}

	private int howManyChangesAtRevisionNumber(AuditReader auditReader, Class<?> clazz, Number revision) {
		return ( (Long) auditReader.createQuery().forEntitiesModifiedAtRevision( clazz, revision )
				.addProjection( AuditEntity.id().count() ).getSingleResult() ).intValue();
	}

	private List<?> findAllAuditedObjects(AuditReader auditReader, Class<?> clazz) {
		return auditReader.createQuery().forRevisionsOfEntity( clazz, true, true ).getResultList();
	}

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
		Query luceneQuery = createLuceneQuery( term, value );
		return session.createFullTextQuery( luceneQuery, Person.class )
				.setSort( new Sort( new SortField( "surname", SortField.STRING ) ) ).list();
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
		String searchQuery = term + ":" + value;
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), term, new StopAnalyzer(TestConstants.getTargetLuceneVersion()) );
		Query luceneQuery;
		try {
			luceneQuery = parser.parse( searchQuery );
		}
		catch ( ParseException e ) {
			throw new RuntimeException("Unable to parse query", e );
		}
		return luceneQuery;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Person.class, Address.class };
	}

	@Entity
	@Indexed
	@Audited(withModifiedFlag = true)
	public static class Person {
		public Person() { }
		public Person(String name, String surname, Address address) {
			super();
			this.name = name;
			this.surname = surname;
			this.address = address;
		}

		@Id @GeneratedValue @DocumentId
		private Long id;
		public Long getId() { return id; }
		public void setId(Long id) { this.id = id; }

		@Field
		private String name;
		public String getName() { return name; }
		public void setName(String name) { this.name = name; }

		@Field
		private String surname;
		public String getSurname() { return surname; }
		public void setSurname(String surname) { this.surname = surname; }

		@ManyToOne @IndexedEmbedded
		private Address address;
		public Address getAddress() { return address; }
		public void setAddress(Address address) { this.address = address; }

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ( ( address == null ) ? 0 : address.getId().hashCode() );
			result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
			result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
			result = prime * result + ( ( surname == null ) ? 0 : surname.hashCode() );
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if ( this == obj )
				return true;
			if ( obj == null )
				return false;
			if ( getClass() != obj.getClass() )
				return false;
			Person other = (Person) obj;
			if ( address == null ) {
				if ( other.address != null )
					return false;
			}
			else if ( !address.getId().equals( other.address.getId() ) )
				return false;
			if ( id == null ) {
				if ( other.id != null )
					return false;
			}
			else if ( !id.equals( other.id ) )
				return false;
			if ( name == null ) {
				if ( other.name != null )
					return false;
			}
			else if ( !name.equals( other.name ) )
				return false;
			if ( surname == null ) {
				if ( other.surname != null )
					return false;
			}
			else if ( !surname.equals( other.surname ) )
				return false;
			return true;
		}
	}

	@Entity
	@Indexed
	@Audited(withModifiedFlag = true)
	public static class Address {
		public Address() { }
		public Address(String streetName, Integer houseNumber) {
			super();
			this.streetName = streetName;
			this.houseNumber = houseNumber;
		}

		@Id @GeneratedValue @DocumentId
		private Long id;
		public Long getId() { return id; }
		public void setId(Long id) { this.id = id; }

		@Field
		private String streetName;
		public String getStreetName() { return streetName; }
		public void setStreetName(String streetName) { this.streetName = streetName; }

		@Field
		private Integer houseNumber;
		public Integer getHouseNumber() { return houseNumber; }
		public void setHouseNumber(Integer houseNumber) { this.houseNumber = houseNumber; }

		@Field
		private Integer flatNumber;
		public Integer getFlatNumber() { return flatNumber; }
		public void setFlatNumber(Integer flatNumber) { this.flatNumber = flatNumber; }

		@OneToMany(mappedBy = "address")
		private Set<Person> persons;
		public Set<Person> getPersons() { return persons; }
		public void setPersons(Set<Person> persons) { this.persons = persons; }

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ( ( flatNumber == null ) ? 0 : flatNumber.hashCode() );
			result = prime * result + ( ( houseNumber == null ) ? 0 : houseNumber.hashCode() );
			result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
			result = prime * result + ( ( persons == null ) ? 0 : persons.hashCode() );
			result = prime * result + ( ( streetName == null ) ? 0 : streetName.hashCode() );
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if ( this == obj )
				return true;
			if ( obj == null )
				return false;
			if ( getClass() != obj.getClass() )
				return false;
			Address other = (Address) obj;
			if ( flatNumber == null ) {
				if ( other.flatNumber != null )
					return false;
			}
			else if ( !flatNumber.equals( other.flatNumber ) )
				return false;
			if ( houseNumber == null ) {
				if ( other.houseNumber != null )
					return false;
			}
			else if ( !houseNumber.equals( other.houseNumber ) )
				return false;
			if ( id == null ) {
				if ( other.id != null )
					return false;
			}
			else if ( !id.equals( other.id ) )
				return false;
			if ( persons == null ) {
				if ( other.persons != null )
					return false;
			}
			else if ( !persons.equals( other.persons ) )
				return false;
			if ( streetName == null ) {
				if ( other.streetName != null )
					return false;
			}
			else if ( !streetName.equals( other.streetName ) )
				return false;
			return true;
		}
	}
}
