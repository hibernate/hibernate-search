/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration;

import static org.junit.Assert.assertEquals;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

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
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;

import org.hibernate.testing.SkipForDialect;

import org.junit.Test;

import org.apache.lucene.search.Query;

/**
 * Tests that a field can be mapped as {@code @Lob}.
 *
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "HSEARCH-993")
public class LobTest extends SearchTestBase {

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
	public Class<?>[] getAnnotatedClasses() {
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

