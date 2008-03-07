//$
package org.hibernate.search.test.id;

import java.util.List;

import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.Search;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.index.Term;

/**
 * @author Emmanuel Bernard
 */
public class EmbeddedIdTest extends SearchTestCase {
	public void testFieldBridge() throws Exception {
		PersonPK emmPk = new PersonPK();
		emmPk.setFirstName( "Emmanuel" );
		emmPk.setLastName( "Bernard" );
		Person emm = new Person();
		emm.setFavoriteColor( "Blue" );
		emm.setId( emmPk );
		Session s = openSession(  );
		Transaction tx = s.beginTransaction();
		s.save(emm);
		tx.commit();
		s.clear();
		tx = s.beginTransaction();
		List results = Search.createFullTextSession( s ).createFullTextQuery(
				new TermQuery( new Term("id.lastName", "Bernard" ) ) ).list();
		assertEquals( 1, results.size() );
		s.delete( results.get( 0 ) );
		tx.commit();
		s.close();

	}
	protected Class[] getMappings() {
		return new Class[] {
				Person.class
		};
	}
}
