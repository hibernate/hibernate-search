/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.engine;

import static org.junit.Assert.assertEquals;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;

import org.junit.Test;

import org.apache.lucene.search.Query;

/**
 * Verify we don't rely on dirtyness values from Hibernate ORM on fields
 * mapped with {@link jakarta.persistence.Transient}.
 *
 * @author Sanne Grinovero (C) 2012 Red Hat Inc.
 */
public class TransientFieldsDirtyTest extends SearchTestBase {

	@TestForIssue(jiraKey = "HSEARCH-1096")
	@Test
	public void testTransientFieldsAreAlwaysDirty() {
		Session session = openSession();
		try {
			FormulaAdd f = new FormulaAdd();
			f.id = 1L;
			f.a = 1;
			f.b = 2;
			Transaction transaction = session.beginTransaction();
			session.persist( f );

			transaction.commit();
			session.clear();

			assertFormulaMatches( "3", session );

			transaction = session.beginTransaction();
			FormulaAdd loadedFormula = (FormulaAdd) session.load( FormulaAdd.class, 1L );
			loadedFormula.a = 3;
			transaction.commit();
			session.clear();

			assertFormulaMatches( "5", session );
		}
		finally {
			session.close();
		}
	}

	private void assertFormulaMatches(String value, Session session) {
		FullTextSession fullTextSession = Search.getFullTextSession( session );
		Transaction transaction = session.beginTransaction();
		QueryBuilder queryBuilder = fullTextSession.getSearchFactory()
				.buildQueryBuilder()
				.forEntity( FormulaAdd.class )
				.get();
		Query luceneQuery = queryBuilder.keyword().onField( "aplusB" ).ignoreAnalyzer().matching( value ).createQuery();
		FullTextQuery query = fullTextSession.createFullTextQuery( luceneQuery, FormulaAdd.class );
		List resultsList = query.list();
		transaction.commit();
		assertEquals( 1, resultsList.size() );
		FormulaAdd result = (FormulaAdd) resultsList.get( 0 );
		assertEquals( value, result.getAplusB() );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { FormulaAdd.class };
	}

	@Indexed
	@Entity
	@Table(name = "FORMULAADD")
	public static class FormulaAdd {

		long id;
		int a;
		int b;

		@Id
		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public int getA() {
			return a;
		}

		public void setA(int a) {
			this.a = a;
		}

		public int getB() {
			return b;
		}

		public void setB(int b) {
			this.b = b;
		}

		@Transient
		@Field(analyze = Analyze.NO)
		@IndexingDependency(derivedFrom = {
				@ObjectPath(@PropertyValue(propertyName = "a")),
				@ObjectPath(@PropertyValue(propertyName = "b"))
		})
		public String getAplusB() {
			return "" + ( a + b );
		}
	}
}
