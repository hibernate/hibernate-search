/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.query.sorting;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import jakarta.persistence.EntityTransaction;

import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.jpa.JPATestCase;
import org.hibernate.search.test.query.ProductArticle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.apache.lucene.search.Sort;

/**
 * @author Davide D'Alto
 */
class SortUsingEntityManagerTest extends JPATestCase {

	private static final TimeZone ENCODING_TIME_ZONE = TimeZone.getTimeZone( "UTC" );

	private FullTextEntityManager em;

	@Override
	@BeforeEach
	public void setUp() {
		super.setUp();
		em = org.hibernate.search.jpa.Search.getFullTextEntityManager( factory.createEntityManager() );
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
	void testResultOrderedByDateDescending() throws Exception {
		EntityTransaction tx = em.getTransaction();
		tx.begin();

		QueryBuilder builder = em.getSearchFactory().buildQueryBuilder().forEntity( ProductArticle.class ).get();
		org.apache.lucene.search.Query query =
				builder.keyword().wildcard().onField( "header" ).matching( "hib*" ).createQuery();
		Sort dateDescending = builder.sort().byField( "creationDate" ).desc().createSort();
		List<ProductArticle> result = em.createFullTextQuery( query, ProductArticle.class )
				.setSort( dateDescending ).setFirstResult( 3 ).getResultList();

		assertThat( result ).as( "query result" ).hasSize( 3 );
		assertThat( result.get( 0 ).getArticleId() ).as( "article id" ).isEqualTo( 3 );
		assertThat( result.get( 1 ).getArticleId() ).as( "article id" ).isEqualTo( 2 );
		assertThat( result.get( 2 ).getArticleId() ).as( "article id" ).isEqualTo( 1 );
		tx.commit();
		em.clear();
	}

	private Date date(int day, int month, int year) {
		Calendar cal = Calendar.getInstance( ENCODING_TIME_ZONE, Locale.US );
		cal.set( year, month, day, 11, 05, 30 );
		return cal.getTime();
	}

	private ProductArticle article(int id, String header, Date date) {
		ProductArticle article = new ProductArticle();
		article.setCreationDate( date );
		article.setHeader( header );
		article.setArticleId( Integer.valueOf( id ) );
		return article;
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { ProductArticle.class };
	}

}
