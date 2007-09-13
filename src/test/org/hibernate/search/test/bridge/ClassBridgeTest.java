package org.hibernate.search.test.bridge;

import java.util.List;

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.Environment;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestCase;

/**
 * @author John Griffin
 */
public class ClassBridgeTest extends SearchTestCase {
	/**
	 * This test checks for two fields being concatentated by the user-supplied
	 * CatFieldsClassBridge class which is specified as the implementation class
	 * in the ClassBridge annotation of the Department class.
	 *
	 * @throws Exception
	 */
	public void testClassBridge() throws Exception {
		org.hibernate.Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( getDept1() );
		s.persist( getDept2() );
		s.persist( getDept3() );
		s.flush();
		tx.commit();

		tx = s.beginTransaction();
		FullTextSession session = Search.createFullTextSession( s );

		// The branchnetwork field is the concatenation of both
		// the branch field and the network field of the Department
		// class. This is in the Lucene document but not in the
		// Department entity itself.
		QueryParser parser = new QueryParser( "branchnetwork", new SimpleAnalyzer() );

		Query query = parser.parse( "branchnetwork:layton 2B" );
		org.hibernate.search.FullTextQuery hibQuery = session.createFullTextQuery( query, Department.class );
		List result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "incorrect entity returned, wrong network", "2B", ( (Department) result.get( 0 ) ).getNetwork() );
		assertEquals( "incorrect entity returned, wrong branch", "Layton", ( (Department) result.get( 0 ) ).getBranch() );
		assertEquals( "incorrect number of results returned", 1, result.size() );

		// Partial match.
		query = parser.parse( "branchnetwork:3c" );
		hibQuery = session.createFullTextQuery( query, Department.class );
		result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "incorrect entity returned, wrong network", "3C", ( (Department) result.get( 0 ) ).getNetwork() );
		assertEquals( "incorrect entity returned, wrong branch", "West Valley", ( (Department) result.get( 0 ) ).getBranch() );
		assertEquals( "incorrect number of results returned", 1, result.size() );

		// No data cross-ups .
		query = parser.parse( "branchnetwork:Kent Lewin" );
		hibQuery = session.createFullTextQuery( query, Department.class );
		result = hibQuery.list();
		assertNotNull( result );
		assertTrue( "problem with field cross-ups", result.size() == 0 );

		// Non-ClassBridge field.
		parser = new QueryParser( "branchHead", new SimpleAnalyzer() );
		query = parser.parse( "branchHead:Kent Lewin" );
		hibQuery = session.createFullTextQuery( query, Department.class );
		result = hibQuery.list();
		assertNotNull( result );
		assertTrue( "incorrect entity returned, wrong branch head", result.size() == 1 );
		assertEquals("incorrect entity returned", "Kent Lewin", ( (Department) result.get( 0 ) ).getBranchHead());

		//cleanup
		for (Object element : s.createQuery( "from " + Department.class.getName() ).list()) s.delete( element );
		tx.commit();
		s.close();
	}

	private Department getDept1() {
		Department dept = new Department();

//		dept.setId( 1000 );
		dept.setBranch( "Salt Lake City" );
		dept.setBranchHead( "Kent Lewin" );
		dept.setMaxEmployees( 100 );
		dept.setNetwork( "1A" );

		return dept;
	}

	private Department getDept2() {
		Department dept = new Department();

//		dept.setId( 1001 );
		dept.setBranch( "Layton" );
		dept.setBranchHead( "Terry Poperszky" );
		dept.setMaxEmployees( 20 );
		dept.setNetwork( "2B" );

		return dept;
	}

	private Department getDept3() {
		Department dept = new Department();

//		dept.setId( 1002 );
		dept.setBranch( "West Valley" );
		dept.setBranchHead( "Pat Kelley" );
		dept.setMaxEmployees( 15 );
		dept.setNetwork( "3C" );

		return dept;
	}

	protected Class[] getMappings() {
		return new Class[] {
				Department.class
		};
	}

	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.ANALYZER_CLASS, SimpleAnalyzer.class.getName() );
	}
}