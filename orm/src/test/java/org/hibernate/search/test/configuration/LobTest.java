/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.configuration;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.apache.lucene.search.Query;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.dialect.Sybase11Dialect;
import org.hibernate.dialect.SybaseASE15Dialect;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestCaseJUnit4;
import org.hibernate.search.test.util.TestForIssue;
import org.hibernate.testing.SkipForDialect;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests that a field can be mapped as {@code @Lob}.
 *
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "HSEARCH-993")
public class LobTest extends SearchTestCaseJUnit4 {

	@Test
	@SkipForDialect(value = { SybaseASE15Dialect.class, Sybase11Dialect.class },
			comment = "Sybase does not support @Lob")
	public void testCreateIndexSearchEntityWithLobField() {
		// create and index
		Session session = openSession();
		Transaction tx = session.beginTransaction();

		LobHolder lobHolder = new LobHolder();
		lobHolder.setVeryLongText( "this text is very long ..." );
		session.persist( lobHolder );

		tx.commit();
		session.close();

		// search
		session = openSession();
		tx = session.beginTransaction();
		FullTextSession fullTextSession = Search.getFullTextSession( session );

		QueryBuilder qb = fullTextSession.getSearchFactory().buildQueryBuilder().forEntity( LobHolder.class ).get();
		Query query = qb.keyword().onField( "veryLongText" ).matching( "text" ).createQuery();

		FullTextQuery hibernateQuery = fullTextSession.createFullTextQuery( query );
		List<LobHolder> result = hibernateQuery.list();
		assertEquals( "We should have a match for the single LobHolder", 1, result.size() );

		tx.commit();
		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				LobHolder.class
		};
	}

	@Entity
	@Indexed
	public static class LobHolder {
		@Id
		@GeneratedValue
		private long id;

		@Field
		@Lob
		private String veryLongText;

		public String getVeryLongText() {
			return veryLongText;
		}

		public void setVeryLongText(String veryLongText) {
			this.veryLongText = veryLongText;
		}
	}
}


