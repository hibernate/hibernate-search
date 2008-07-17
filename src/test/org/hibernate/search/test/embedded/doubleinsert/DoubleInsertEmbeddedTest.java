// $Id$
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

		s = Search.getFullTextSession( openSession( ) );
		s.getTransaction().begin();
		Term term = new Term("county", "county");
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

	protected Class[] getMappings() {
		return new Class[] {
				Address.class,
				Contact.class,
				PersonalContact.class,
				BusinessContact.class,
				Phone.class
		};
	}
}
