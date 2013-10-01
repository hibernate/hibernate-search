/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.worker;

import java.util.List;

import junit.framework.Assert;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.hibernate.Session;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestCaseJUnit4;
import org.hibernate.search.test.util.TestForIssue;
import org.junit.Test;

/**
 * Tests functionality of method {@link org.hibernate.search.FullTextSession#clearIndexingQueue}
 *
 * @author Sanne Grinovero
 */
@TestForIssue(jiraKey = "HSEARCH-1425")
public class QueueClearedTestCase extends SearchTestCaseJUnit4 {

	@Test
	public void testTransactionalSessionClear() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );

		s.getTransaction().begin();
		persistDrink( s, "Water" );
		s.clearIndexingQueue();

		persistDrink( s, "Espresso" );
		s.getTransaction().commit();

		s.clear();

		List list = s.createFullTextQuery( new MatchAllDocsQuery() ).setProjection( "name" ).list();
		Assert.assertEquals( 1, list.size() );
		Assert.assertEquals( "Espresso", ( (Object[])list.get( 0 ) )[0] );
	}

	private void persistDrink(Session s, String drinkName) {
		Drink d = new Drink();
		d.setName( drinkName );
		s.persist( d );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Drink.class };
	}

}
