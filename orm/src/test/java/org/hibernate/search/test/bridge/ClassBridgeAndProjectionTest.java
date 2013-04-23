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

import org.apache.lucene.queryParser.QueryParser;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.TestConstants;

/**
 * @author Emmanuel Bernard
 */
public class ClassBridgeAndProjectionTest extends SearchTestCase {

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
