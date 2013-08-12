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
package org.hibernate.search.test.bridge;

import java.util.List;

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.document.Document;
import org.hibernate.Transaction;
import org.hibernate.ScrollableResults;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.Environment;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.TestConstants;

/**
 * @author John Griffin
 */
public class ClassBridgeTest extends SearchTestCase {

	/**
	 * This tests that a field created by a user-supplied
	 * EquipmentType class has been created and is a translation
	 * from an identifier to a manufacturer name.
	 *
	 * @throws Exception
	 */
	public void testClassBridges() throws Exception {
		org.hibernate.Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( getDepts1() );
		s.persist( getDepts2() );
		s.persist( getDepts3() );
		s.persist( getDepts4() );
		s.flush();
		tx.commit();

		tx = s.beginTransaction();
		FullTextSession session = Search.getFullTextSession( s );

		// The equipment field is the manufacturer field in the
		// Departments entity after being massaged by passing it
		// through the EquipmentType class. This field is in
		// the Lucene document but not in the Department entity itself.
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "equipment", TestConstants.simpleAnalyzer );

		// Check the second ClassBridge annotation
		Query query = parser.parse( "equiptype:Cisco" );
		org.hibernate.search.FullTextQuery hibQuery = session.createFullTextQuery( query, Departments.class );
		List<Departments> result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "incorrect number of results returned", 2, result.size() );
		for ( Departments d : result ) {
			assertEquals( "incorrect manufacturer", "C", d.getManufacturer() );
		}

		// No data cross-ups.
		query = parser.parse( "branchnetwork:Kent Lewin" );
		hibQuery = session.createFullTextQuery( query, Departments.class );
		result = hibQuery.list();
		assertNotNull( result );
		assertTrue( "problem with field cross-ups", result.size() == 0 );

		// Non-ClassBridge field.
		parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "branchHead", TestConstants.simpleAnalyzer );
		query = parser.parse( "branchHead:Kent Lewin" );
		hibQuery = session.createFullTextQuery( query, Departments.class );
		result = hibQuery.list();
		assertNotNull( result );
		assertTrue( "incorrect entity returned, wrong branch head", result.size() == 1 );
		assertEquals( "incorrect entity returned", "Kent Lewin", ( result.get( 0 ) ).getBranchHead() );

		// Check other ClassBridge annotation.
		parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "branchnetwork", TestConstants.simpleAnalyzer );
		query = parser.parse( "branchnetwork:st. george 1D" );
		hibQuery = session.createFullTextQuery( query, Departments.class );
		result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "incorrect entity returned, wrong network", "1D", ( result.get( 0 ) ).getNetwork() );
		assertEquals( "incorrect entity returned, wrong branch", "St. George", ( result.get( 0 ) ).getBranch() );
		assertEquals( "incorrect number of results returned", 1, result.size() );

		// cleanup
		for ( Object element : s.createQuery( "from " + Departments.class.getName() ).list() ) {
			s.delete( element );
		}
		tx.commit();
		s.close();
	}

	/**
	 * This is the same test as above with a projection query
	 * to show the presence of the ClassBridge impl built fields
	 * just in case you don't believe us.
	 *
	 * @throws Exception
	 */
	public void testClassBridgesWithProjection() throws Exception {
		org.hibernate.Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( getDepts1() );
		s.persist( getDepts2() );
		s.persist( getDepts3() );
		s.persist( getDepts4() );
		s.flush();
		tx.commit();

		tx = s.beginTransaction();
		FullTextSession session = Search.getFullTextSession( s );

		// The equipment field is the manufacturer field  in the
		// Departments entity after being massaged by passing it
		// through the EquipmentType class. This field is in
		// the Lucene document but not in the Department entity itself.
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "equipment", TestConstants.simpleAnalyzer );

		// Check the second ClassBridge annotation
		Query query = parser.parse( "equiptype:Cisco" );
		org.hibernate.search.FullTextQuery hibQuery = session.createFullTextQuery( query, Departments.class );

		hibQuery.setProjection( FullTextQuery.THIS, FullTextQuery.DOCUMENT );

		ScrollableResults projections = hibQuery.scroll();
		assertNotNull( projections );

		projections.beforeFirst();
		projections.next();
		Object[] projection = projections.get();

		assertTrue( "DOCUMENT incorrect", projection[0] instanceof Departments );
		assertEquals( "id incorrect", 1, ((Departments)projection[0]).getId() );
		assertTrue( "DOCUMENT incorrect", projection[1] instanceof Document );
		assertEquals( "DOCUMENT size incorrect", 8, ( (Document) projection[1] ).getFields().size() );
		assertNotNull( "equiptype is null", ( (Document) projection[1] ).getFieldable( "equiptype" ) );
		assertEquals( "equiptype incorrect", "Cisco", ( (Document) projection[1] ).getFieldable( "equiptype" ).stringValue() );
		assertNotNull( "branchnetwork is null", ( (Document) projection[1] ).getFieldable( "branchnetwork" ) );
		assertEquals( "branchnetwork incorrect", "Salt Lake City 1A", ( (Document) projection[1] ).getFieldable( "branchnetwork" ).stringValue() );

		projections.next();
		projection = projections.get();

		assertTrue( "DOCUMENT incorrect", projection[0] instanceof Departments );
		assertEquals( "id incorrect", 4, ((Departments)projection[0]).getId() );
		assertTrue( "DOCUMENT incorrect", projection[1] instanceof Document );
		assertEquals( "DOCUMENT size incorrect", 8, ( (Document) projection[1] ).getFields().size() );
		assertNotNull( "equiptype is null", ( (Document) projection[1] ).getFieldable( "equiptype" ) );
		assertEquals( "equiptype incorrect", "Cisco", ( (Document) projection[1] ).getFieldable( "equiptype" ).stringValue() );
		assertNotNull( "branchnetwork is null", ( (Document) projection[1] ).getFieldable( "branchnetwork" ) );
		assertEquals( "branchnetwork incorrect", "St. George 1D", ( (Document) projection[1] ).getFieldable( "branchnetwork" ).stringValue() );

		assertTrue( "incorrect result count returned", projections.isLast() );
		//cleanup
		for ( Object element : s.createQuery( "from " + Departments.class.getName() ).list() ) {
			s.delete( element );
		}
		tx.commit();
		s.close();
	}

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
		FullTextSession session = Search.getFullTextSession( s );

		// The branchnetwork field is the concatenation of both
		// the branch field and the network field of the Department
		// class. This is in the Lucene document but not in the
		// Department entity itself.
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "branchnetwork", TestConstants.simpleAnalyzer );

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
		parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "branchHead", TestConstants.simpleAnalyzer );
		query = parser.parse( "branchHead:Kent Lewin" );
		hibQuery = session.createFullTextQuery( query, Department.class );
		result = hibQuery.list();
		assertNotNull( result );
		assertTrue( "incorrect entity returned, wrong branch head", result.size() == 1 );
		assertEquals( "incorrect entity returned", "Kent Lewin", ( (Department) result.get( 0 ) ).getBranchHead() );

		//cleanup
		for ( Object element : s.createQuery( "from " + Department.class.getName() ).list() ) {
			s.delete( element );
		}
		tx.commit();
		s.close();
	}

	private Department getDept1() {
		Department dept = new Department();

		dept.setBranch( "Salt Lake City" );
		dept.setBranchHead( "Kent Lewin" );
		dept.setMaxEmployees( 100 );
		dept.setNetwork( "1A" );
		return dept;
	}

	private Department getDept2() {
		Department dept = new Department();

		dept.setBranch( "Layton" );
		dept.setBranchHead( "Terry Poperszky" );
		dept.setMaxEmployees( 20 );
		dept.setNetwork( "2B" );

		return dept;
	}

	private Department getDept3() {
		Department dept = new Department();

		dept.setBranch( "West Valley" );
		dept.setBranchHead( "Pat Kelley" );
		dept.setMaxEmployees( 15 );
		dept.setNetwork( "3C" );

		return dept;
	}

	private Departments getDepts1() {
		Departments depts = new Departments();

		depts.setBranch( "Salt Lake City" );
		depts.setBranchHead( "Kent Lewin" );
		depts.setMaxEmployees( 100 );
		depts.setNetwork( "1A" );
		depts.setManufacturer( "C" );

		return depts;
	}

	private Departments getDepts2() {
		Departments depts = new Departments();

		depts.setBranch( "Layton" );
		depts.setBranchHead( "Terry Poperszky" );
		depts.setMaxEmployees( 20 );
		depts.setNetwork( "2B" );
		depts.setManufacturer( "3" );

		return depts;
	}

	private Departments getDepts3() {
		Departments depts = new Departments();

		depts.setBranch( "West Valley" );
		depts.setBranchHead( "Pat Kelley" );
		depts.setMaxEmployees( 15 );
		depts.setNetwork( "3C" );
		depts.setManufacturer( "D" );

		return depts;
	}

	private Departments getDepts4() {
		Departments depts = new Departments();

		depts.setBranch( "St. George" );
		depts.setBranchHead( "Spencer Stajskal" );
		depts.setMaxEmployees( 10 );
		depts.setNetwork( "1D" );
		depts.setManufacturer( "C" );
		return depts;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Department.class,
				Departments.class
		};
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.ANALYZER_CLASS, SimpleAnalyzer.class.getName() );
	}
}
