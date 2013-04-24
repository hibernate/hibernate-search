/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.engine;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import junit.framework.Assert;
import org.apache.lucene.search.Query;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.util.TestForIssue;

/**
 * Verify we don't rely on dirtyness values from Hibernate ORM on fields
 * mapped with {@link javax.persistence.Transient}.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class TransientFieldsDirtyTest extends SearchTestCase {

	@TestForIssue(jiraKey = "HSEARCH-1096")
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
		Assert.assertEquals( 1, resultsList.size() );
		FormulaAdd result = (FormulaAdd) resultsList.get( 0 );
		Assert.assertEquals( value, result.getAplusB() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
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
		public String getAplusB() {
			return "" + ( a + b );
		}
	}
}
