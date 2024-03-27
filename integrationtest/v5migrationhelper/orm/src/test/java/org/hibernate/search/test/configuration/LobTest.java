/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.DatabaseContainer;

import org.junit.jupiter.api.Test;

import org.apache.lucene.search.Query;

/**
 * Tests that a field can be mapped as {@code @Lob}.
 *
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "HSEARCH-993")
class LobTest extends SearchTestBase {

	@Test
	void testCreateIndexSearchEntityWithLobField() {
		assumeFalse(
				org.hibernate.dialect.SybaseDialect.class.getName().equals( DatabaseContainer.configuration().driver() ),
				"Sybase does not support @Lob"
		);
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
		assertThat( result ).as( "We should have a match for the single LobHolder" ).hasSize( 1 );

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

