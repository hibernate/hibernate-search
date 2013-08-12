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
package org.hibernate.search.test.embedded.doubleinsert;

import java.util.Date;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.hibernate.Query;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestCase;

/**
 * @author Emmanuel Bernard
 */
public class DoubleInsertEmbeddedTest extends SearchTestCase {

	public void testDoubleInsert() throws Exception {
		PersonalContact contact = createTestData();
		FullTextSession s = Search.getFullTextSession( openSession( ) );
		s.getTransaction().begin();
		Term term = new Term( "county", "county" );
		TermQuery termQuery = new TermQuery( term );
		Query query = s.createFullTextQuery( termQuery );
		assertEquals( 1, query.list().size() );
		contact = (PersonalContact) s.get( PersonalContact.class, contact.getId() );
		contact.getPhoneNumbers().clear();
		contact.getAddresses().clear();
		s.flush();
		s.clear();
		s.createQuery( "delete " + Address.class.getName() ).executeUpdate();
		s.createQuery( "delete " + Phone.class.getName() ).executeUpdate();
		s.createQuery( "delete " + Contact.class.getName() ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	public void testMultipleUpdatesTriggeredByContainedIn() {
		PersonalContact contact = createTestData();
		FullTextSession s = Search.getFullTextSession( openSession( ) );
		s.getTransaction().begin();
		contact = (PersonalContact) s.load( PersonalContact.class, contact.getId() );
		contact.setEmail( "spam@hibernate.org" );
		s.getTransaction().commit();
		s.close();
	}

	public PersonalContact createTestData() {
		Address address = new Address();
		address.setAddress1( "TEST1" );
		address.setAddress2( "N/A" );
		address.setTown( "TEST TOWN" );
		address.setCounty( "TEST COUNTY" );
		address.setCountry( "UK" );
		address.setPostcode( "XXXXXXX" );
		address.setActive( true );
		address.setCreatedOn( new Date() );
		address.setLastUpdatedOn( new Date() );

		Phone phone = new Phone();
		phone.setNumber( "01273234122" );
		phone.setType( "HOME" );
		phone.setCreatedOn( new Date() );
		phone.setLastUpdatedOn( new Date() );

		PersonalContact contact = new PersonalContact();
		contact.setFirstname( "Amin" );
		contact.setSurname( "Mohammed-Coleman" );
		contact.setEmail( "address@hotmail.com" );
		contact.setDateOfBirth( new Date() );
		contact.setNotifyBirthDay( false );
		contact.setCreatedOn( new Date() );
		contact.setLastUpdatedOn( new Date() );
		contact.setNotes( "TEST" );
		contact.addAddressToContact( address );
		contact.addPhoneToContact( phone );

		FullTextSession s = Search.getFullTextSession( openSession( ) );
		s.getTransaction().begin();
		s.save( contact);
		s.getTransaction().commit();

		s.close();
		return contact;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Phone.class,
				Contact.class,
				PersonalContact.class,
				Address.class,
				BusinessContact.class,
		};
	}
}
