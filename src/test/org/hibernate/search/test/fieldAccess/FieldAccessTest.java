//$Id$
package org.hibernate.search.test.fieldAccess;

import java.util.List;

import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

/**
 * @author Emmanuel Bernard
 */
public class FieldAccessTest extends SearchTestCase {

    public void testFields() throws Exception {
        Document doc = new Document( "Hibernate in Action", "Object/relational mapping with Hibernate", "blah blah blah" );
        Session s = openSession();
        Transaction tx = s.beginTransaction();
        s.persist( doc );
        tx.commit();

        s.clear();

        FullTextSession session = Search.getFullTextSession(s);
        tx = session.beginTransaction();
        QueryParser p = new QueryParser("id", new StandardAnalyzer( ) );
        List result = session.createFullTextQuery( p.parse( "Abstract:Hibernate" ) ).list();
        assertEquals( "Query by field", 1, result.size() );
        s.delete( result.get( 0 ) );
        tx.commit();
        s.close();

    }

    public void testFieldBoost() throws Exception {
        Session s = openSession();
        Transaction tx = s.beginTransaction();
        s.persist(
				new Document( "Hibernate in Action", "Object and Relational", "blah blah blah" )
		);
		s.persist(
				new Document( "Object and Relational", "Hibernate in Action", "blah blah blah" )
		);
        tx.commit();

        s.clear();

        FullTextSession session = Search.getFullTextSession(s);
        tx = session.beginTransaction();
        QueryParser p = new QueryParser("id", new StandardAnalyzer( ) );
        List result = session.createFullTextQuery( p.parse( "title:Action OR Abstract:Action" ) ).list();
        assertEquals( "Query by field", 2, result.size() );
        assertEquals( "@Boost fails", "Hibernate in Action", ( (Document) result.get( 0 ) ).getTitle() );
        s.delete( result.get( 0 ) );
        tx.commit();
        s.close();

    }

    protected Class[] getMappings() {
        return new Class[] {
                Document.class
        };
    }
}
