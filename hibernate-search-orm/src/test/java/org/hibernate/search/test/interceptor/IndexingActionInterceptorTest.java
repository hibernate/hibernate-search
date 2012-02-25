/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.test.interceptor;

import org.apache.lucene.search.Query;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestCase;

import static org.fest.assertions.Assertions.*;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class IndexingActionInterceptorTest extends SearchTestCase {

	public void testSoftDelete() throws Exception {
		Blog blog = new Blog();
		blog.setTitle( "Hibernate Search now has soft deletes!" );
		blog.setStatus( BlogStatus.DRAFT );

		Article article = new Article();
		article.setTitle( "Hibernate Search: detailed description of soft deletes" );
		article.setStatus( BlogStatus.DRAFT );

		TotalArticle totalArticle = new TotalArticle();
		totalArticle.setTitle( "Hibernate Search: the total truth about soft deletes" );
		totalArticle.setStatus( BlogStatus.DRAFT );

		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		s.persist( blog );
		s.persist( article );
		s.persist( totalArticle );
		tx.commit();

		s.clear();

		tx = s.beginTransaction();
		QueryBuilder b = s.getSearchFactory().buildQueryBuilder().forEntity( Blog.class ).get();
		Query blogQuery = b.keyword().onField( "title" ).matching( "now" ).createQuery();
		Query articleQuery = b.keyword().onField( "title" ).matching( "detailed" ).createQuery();
		Query totalArticleQuery = b.keyword().onField( "title" ).matching( "truth" ).createQuery();
		assertThat( s.createFullTextQuery( blogQuery, Blog.class ).list() ).as("Blog is explicit intercepted").hasSize( 0 );
		assertThat( s.createFullTextQuery( articleQuery, Blog.class ).list() ).as("Article is inherently intercepted").hasSize( 0 );
		assertThat( s.createFullTextQuery( totalArticleQuery, Blog.class ).list() ).as("TotalArticle is explicit not intercepted").hasSize( 1 );
		blog = (Blog) s.get( Blog.class, blog.getId() );
		blog.setStatus( BlogStatus.PUBLISHED );
		article = (Article) s.get( Article.class, article.getId() );
		article.setStatus( BlogStatus.PUBLISHED );
		totalArticle = (TotalArticle) s.get( TotalArticle.class, totalArticle.getId() );
		totalArticle.setStatus( BlogStatus.PUBLISHED );
		tx.commit();

		s.clear();

		tx = s.beginTransaction();
		assertThat( s.createFullTextQuery( blogQuery, Blog.class ).list() ).hasSize( 1 );
		assertThat( s.createFullTextQuery( articleQuery, Blog.class ).list() ).as("Article is inherently intercepted").hasSize( 1 );
		assertThat( s.createFullTextQuery( totalArticleQuery, Blog.class ).list() ).as("TotalArticle is explicit not intercepted").hasSize( 1 );
		blog = (Blog) s.get( Blog.class, blog.getId() );
		blog.setStatus( BlogStatus.REMOVED );
		article = (Article) s.get( Article.class, article.getId() );
		article.setStatus( BlogStatus.REMOVED );
		totalArticle = (TotalArticle) s.get( TotalArticle.class, totalArticle.getId() );
		totalArticle.setStatus( BlogStatus.REMOVED );
		tx.commit();

		s.clear();

		tx = s.beginTransaction();
		assertThat( s.createFullTextQuery( blogQuery, Blog.class ).list() ).hasSize( 0 );
		assertThat( s.createFullTextQuery( articleQuery, Blog.class ).list() ).as("Article is inherently intercepted").hasSize( 0 );
		assertThat( s.createFullTextQuery( totalArticleQuery, Blog.class ).list() ).as("TotalArticle is explicit not intercepted").hasSize( 1 );
		blog = (Blog) s.get( Blog.class, blog.getId() );
		s.delete( blog );
		blog = (Blog) s.get( Article.class, article.getId() );
		s.delete( blog );
		blog = (Blog) s.get( TotalArticle.class, totalArticle.getId() );
		s.delete( blog );
		tx.commit();

		s.close();

	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Blog.class,
				Article.class,
				TotalArticle.class
		};
	}
}
