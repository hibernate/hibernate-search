//$Id$
package org.hibernate.search.test.bridge;

import java.util.Date;
import java.util.List;
import java.util.GregorianCalendar;
import java.util.Calendar;
import java.util.TimeZone;

import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.Environment;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.apache.lucene.search.Query;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;

/**
 * @author Emmanuel Bernard
 */
public class BridgeTest extends SearchTestCase {
    public void testDefaultAndNullBridges() throws Exception {
        Cloud cloud = new Cloud();
        cloud.setDate( null );
        cloud.setDouble1( null );
        cloud.setDouble2( 2.1d );
        cloud.setInt1( null );
        cloud.setInt2( 2 );
        cloud.setFloat1( null );
        cloud.setFloat2( 2.1f );
        cloud.setLong1( null );
        cloud.setLong2( 2l );
        cloud.setString(null);
		cloud.setType( CloudType.DOG );
		cloud.setStorm( false );
		org.hibernate.Session s = openSession();
        Transaction tx = s.beginTransaction();
        s.persist(cloud);
        s.flush();
        tx.commit();

        tx = s.beginTransaction();
        FullTextSession session = Search.createFullTextSession(s);
        QueryParser parser = new QueryParser("id", new StandardAnalyzer() );
        Query query;
        List result;

        query = parser.parse("double2:[2.1 TO 2.1] AND float2:[2.1 TO 2.1] " +
				"AND int2:[2 TO 2.1] AND long2:[2 TO 2.1] AND type:\"dog\" AND storm:false");

		result = session.createFullTextQuery(query).list();
        assertEquals( "find primitives and do not fail on null", 1, result.size() );

        query = parser.parse("double1:[2.1 TO 2.1] OR float1:[2.1 TO 2.1] OR int1:[2 TO 2.1] OR long1:[2 TO 2.1]");
        result = session.createFullTextQuery(query).list();
        assertEquals( "null elements should not be stored", 0, result.size() ); //the query is dumb because restrictive

		query = parser.parse("type:dog");
        result = session.createFullTextQuery(query).setProjection( "type" ).list();
        assertEquals( "Enum projection works", 1, result.size() ); //the query is dumb because restrictive

		s.delete( s.get( Cloud.class, cloud.getId() ) );
        tx.commit();
        s.close();

    }

    public void testCustomBridges() throws Exception {
        Cloud cloud = new Cloud();
        cloud.setCustomFieldBridge( "This is divided by 2");
        cloud.setCustomStringBridge( "This is div by 4");
        org.hibernate.Session s = openSession();
        Transaction tx = s.beginTransaction();
        s.persist(cloud);
        s.flush();
        tx.commit();

        tx = s.beginTransaction();
        FullTextSession session = Search.createFullTextSession(s);
        QueryParser parser = new QueryParser("id", new SimpleAnalyzer() );
        Query query;
        List result;

        query = parser.parse("customFieldBridge:This AND customStringBridge:This");
        result = session.createFullTextQuery(query).list();
        assertEquals( "Properties not mapped", 1, result.size() );

        query = parser.parse("customFieldBridge:by AND customStringBridge:is");
        result = session.createFullTextQuery(query).list();
        assertEquals( "Custom types not taken into account", 0, result.size() );

        s.delete( s.get( Cloud.class, cloud.getId() ) );
        tx.commit();
        s.close();

    }

    public void testDateBridge() throws Exception {
        Cloud cloud = new Cloud();
        Calendar c = GregorianCalendar.getInstance();
        c.setTimeZone( TimeZone.getTimeZone( "GMT" ) ); //for the sake of tests
        c.set(2000, 11, 15, 3, 43, 2);
        c.set( Calendar.MILLISECOND, 5 );

        Date date = new Date( c.getTimeInMillis() );
        cloud.setDate( date ); //5 millisecond
        cloud.setDateDay( date );
        cloud.setDateHour( date );
        cloud.setDateMillisecond( date );
        cloud.setDateMinute( date );
        cloud.setDateMonth( date );
        cloud.setDateSecond( date );
        cloud.setDateYear( date );
        org.hibernate.Session s = openSession();
        Transaction tx = s.beginTransaction();
        s.persist(cloud);
        s.flush();
        tx.commit();

        tx = s.beginTransaction();
        FullTextSession session = Search.createFullTextSession(s);
        QueryParser parser = new QueryParser("id", new StandardAnalyzer() );
        Query query;
        List result;

        query = parser.parse("date:[19900101 TO 20060101]"
                + " AND dateDay:[20001214 TO 2000121501]"
                + " AND dateMonth:[200012 TO 20001201]"
                + " AND dateYear:[2000 TO 200001]"
                + " AND dateHour:[20001214 TO 2000121503]"
                + " AND dateMinute:[20001214 TO 200012150343]"
                + " AND dateSecond:[20001214 TO 20001215034302]"
                + " AND dateMillisecond:[20001214 TO 20001215034302005]"
        );
        result = session.createFullTextQuery(query).list();
        assertEquals( "Date not found or not property truncated", 1, result.size() );

        s.delete( s.get( Cloud.class, cloud.getId() ) );
        tx.commit();
        s.close();

    }
    protected Class[] getMappings() {
        return new Class[] {
                Cloud.class
        };
    }


    protected void configure(Configuration cfg) {
        super.configure( cfg );
        cfg.setProperty( Environment.ANALYZER_CLASS, SimpleAnalyzer.class.getName() );
    }
}
