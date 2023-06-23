/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.doubleinsert;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.hibernate.query.Query;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;

import org.junit.Test;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

/**
 * @author Emmanuel Bernard
 */
public class DoubleInsertEmbeddedTest extends SearchTestBase {

	@Test
	public void testDoubleInsert() throws Exception {
		PersonalContact contact = createTestData();
		FullTextSession s = Search.getFullTextSession( openSession() );
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

	@Test
	public void testMultipleUpdatesTriggeredByContainedIn() {
		PersonalContact contact = createTestData();
		FullTextSession s = Search.getFullTextSession( openSession() );
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

		FullTextSession s = Search.getFullTextSession( openSession() );
		s.getTransaction().begin();
		s.save( contact );
		s.getTransaction().commit();

		s.close();
		return contact;
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Phone.class,
				Contact.class,
				PersonalContact.class,
				Address.class,
				BusinessContact.class,
		};
	}
}
