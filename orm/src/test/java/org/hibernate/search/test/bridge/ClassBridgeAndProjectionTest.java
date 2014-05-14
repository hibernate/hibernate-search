/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.bridge;

import java.util.List;

import org.apache.lucene.queryparser.classic.QueryParser;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Emmanuel Bernard
 */
public class ClassBridgeAndProjectionTest extends SearchTestBase {

	@Test
	public void testClassBridgeProjection() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		// create entities
		Teacher teacher = new Teacher();
		teacher.setName( "John Smith" );
		s.persist( teacher );

		Student student1 = new Student();
		student1.setGrade( "foo" );
		student1.setName( "Jack Miller" );
		student1.setTeacher( teacher );
		teacher.getStudents().add( student1 );
		s.persist( student1 );

		Student student2 = new Student();
		student2.setGrade( "bar" );
		student2.setName( "Steve Marshall" );
		student2.setTeacher( teacher );
		teacher.getStudents().add( student2 );
		s.persist( student2 );

		tx.commit();

		tx = s.beginTransaction();
		// test query without projection
		FullTextSession ftSession = Search.getFullTextSession( s );
		QueryParser parser = new QueryParser(
				TestConstants.getTargetLuceneVersion(),
				"name",
				TestConstants.standardAnalyzer
		);
		FullTextQuery query = ftSession.createFullTextQuery( parser.parse( "name:John" ), Teacher.class );
		List results = query.list();
		assertNotNull( results );
		assertTrue( results.size() == 1 );

		// now test with projection
		query.setProjection( "amount_of_students" );
		results = query.list();
		assertNotNull( results );
		assertTrue( results.size() == 1 );
		Object[] firstResult = (Object[]) results.get( 0 );
		Integer amountStudents = (Integer) firstResult[0];
		assertEquals( Integer.valueOf( 2 ), amountStudents );
		tx.commit();
		s.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Student.class,
				Teacher.class
		};
	}
}
