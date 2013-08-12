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
package org.hibernate.search.test.query;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.persistence.EntityTransaction;

import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.test.TestConstants;
import org.hibernate.search.test.jpa.JPATestCase;
import org.junit.Test;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class SortUsingEntityManagerTest extends JPATestCase {

	private static final boolean DESC = true;

	private FullTextEntityManager em;
	private QueryParser queryParser;

	@Override
	public void setUp() {
		super.setUp();
		em = org.hibernate.search.jpa.Search.getFullTextEntityManager( factory.createEntityManager() );
		queryParser = new QueryParser( TestConstants.getTargetLuceneVersion(), "header", TestConstants.stopAnalyzer );
		createArticles();
	}

	private void createArticles() {
		EntityTransaction tx = em.getTransaction();
		tx.begin();
		try {
			em.persist( article( 1, "Hibernate & Lucene", date( 4, Calendar.JULY, 2005 ) ) );
			em.persist( article( 2, "Hibernate Search", date( 2, Calendar.SEPTEMBER, 2005 ) ) );
			em.persist( article( 3, "Hibernate OGM", date( 4, Calendar.SEPTEMBER, 2005 ) ) );
			em.persist( article( 4, "Hibernate", date( 4, Calendar.DECEMBER, 2005 ) ) );
			em.persist( article( 5, "Hibernate Validator", date( 8, Calendar.SEPTEMBER, 2010 ) ) );
			em.persist( article( 6, "Hibernate Core", date( 4, Calendar.SEPTEMBER, 2012 ) ) );
		}
		finally {
			tx.commit();
			em.clear();
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testResultOrderedByDateDescending() throws Exception {
		EntityTransaction tx = em.getTransaction();
		tx.begin();
		Sort dateDescending = new Sort( new SortField( "creationDate", SortField.STRING, DESC ) );
		List<ProductArticle> result = query( "Hib*" ).setSort( dateDescending ).setFirstResult( 3 ).getResultList();

		assertThat( result ).as( "query result" ).hasSize( 3 );
		assertThat( result.get( 0 ).getArticleId() ).as( "article id" ).isEqualTo( 3 );
		assertThat( result.get( 1 ).getArticleId() ).as( "article id" ).isEqualTo( 2 );
		assertThat( result.get( 2 ).getArticleId() ).as( "article id" ).isEqualTo( 1 );
		tx.commit();
		em.clear();
	}

	@Override
	public void tearDown() {
		deleteArticles();
		super.tearDown();
	}

	private FullTextQuery query(String q) throws ParseException {
		org.apache.lucene.search.Query query = queryParser.parse( q );
		return em.createFullTextQuery( query, ProductArticle.class );
	}

	private Date date(int day, int month, int year) {
		Calendar cal = Calendar.getInstance( Locale.US );
		cal.set( year, month, day, 11, 05, 30 );
		Date date = cal.getTime();
		return date;
	}

	private ProductArticle article(int id, String header, Date date) {
		ProductArticle article = new ProductArticle();
		article.setCreationDate( date );
		article.setHeader( header );
		article.setArticleId( Integer.valueOf( id ) );
		return article;
	}

	private void deleteArticles() {
		EntityTransaction tx = em.getTransaction();
		tx.begin();
		em.createQuery( "delete " + ProductArticle.class.getName() ).executeUpdate();
		tx.commit();
		em.clear();
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] { ProductArticle.class };
	}

}
