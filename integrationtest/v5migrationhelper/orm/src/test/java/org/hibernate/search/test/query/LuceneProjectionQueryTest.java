/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.testsupport.junit.Tags;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

/**
 * Tests aspects of projection that are specific to the Lucene Backend.
 *
 * @author Emmanuel Bernard
 * @author John Griffin
 * @author Hardy Ferentschik
 */
@Tag(Tags.SKIP_ON_ELASTICSEARCH) // This test is specific to the Lucene backend
class LuceneProjectionQueryTest extends SearchTestBase {

	@BeforeEach
	@Override
	public void setUp() throws Exception {
		super.setUp();
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		Employee e1 = new Employee( 1000, "Griffin", "ITech" );
		s.save( e1 );
		Employee e2 = new Employee( 1001, "Jackson", "Accounting" );
		e2.setHireDate( new Date() );
		s.save( e2 );
		Employee e3 = new Employee( 1002, "Jimenez", "ITech" );
		s.save( e3 );
		Employee e4 = new Employee( 1003, "Stejskal", "ITech" );
		s.save( e4 );
		Employee e5 = new Employee( 1004, "Whetbrook", "ITech" );
		s.save( e5 );

		tx.commit();
		s.clear();
	}

	@AfterEach
	@Override
	public void tearDown() throws Exception {
		Session s = getSession(); // Opened during setup
		try {
			Transaction tx = s.beginTransaction();
			for ( Object element : s.createQuery( "from " + Employee.class.getName() ).list() ) {
				s.delete( element );
			}
			tx.commit();
		}
		finally {
			s.close();
		}
		super.tearDown();
	}

	@Test
	void testLuceneDocumentProjection() throws Exception {
		FullTextSession s = Search.getFullTextSession( getSession() );
		Transaction tx = s.beginTransaction();

		QueryParser parser = new QueryParser( "dept", TestConstants.standardAnalyzer );
		Query query = parser.parse( "dept:Accounting" );
		org.hibernate.search.FullTextQuery hibQuery = s.createFullTextQuery( query, Employee.class );
		hibQuery.setProjection( FullTextQuery.DOCUMENT );

		List<?> result = hibQuery.list();
		assertThat( result ).isNotNull();
		Object[] projection = (Object[]) result.get( 0 );
		assertThat( projection[0] ).as( "DOCUMENT incorrect" ).isInstanceOf( Document.class );
		assertThat( ( (Document) projection[0] ).getFields() ).as( "DOCUMENT size incorrect" ).hasSize( 4 );

		tx.commit();
	}

	@Test
	void testLuceneDocumentProjectionNonLoadedFieldOptimization() throws Exception {
		FullTextSession s = Search.getFullTextSession( getSession() );
		Transaction tx = s.beginTransaction();

		QueryParser parser = new QueryParser( "dept", TestConstants.standardAnalyzer );
		Query query = parser.parse( "dept:Accounting" );
		org.hibernate.search.FullTextQuery hibQuery = s.createFullTextQuery( query, Employee.class );
		hibQuery.setProjection( FullTextQuery.ID, FullTextQuery.DOCUMENT );

		List<?> result = hibQuery.list();
		assertThat( result ).isNotNull();

		Object[] projection = (Object[]) result.get( 0 );
		assertThat( result ).isNotNull();
		assertThat( projection[0] ).as( "id field name not projected" ).isEqualTo( 1001 );
		assertThat(
				( (Document) projection[1] ).getField( "lastname" ).stringValue()
		).as( "Document fields should not be lazy on DOCUMENT projection" ).isEqualTo( "Jackson" );
		assertThat( ( (Document) projection[1] ).getFields() ).as( "DOCUMENT size incorrect" ).hasSize( 4 );

		tx.commit();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Employee.class
		};
	}

}
