/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.interceptor;

import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.TermQuery;

import org.hibernate.Transaction;

import org.hibernate.search.FullTextSession;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 * @author Hardy Ferentschik
 */
public class IndexingActionInterceptorTest extends SearchTestBase {

	private FullTextSession fullTextSession;
	private Blog blog;
	private Article article;
	TotalArticle totalArticle;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		createPersistAndIndexTestData();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		if ( !fullTextSession.getTransaction().isActive() ) {
			Transaction tx = fullTextSession.beginTransaction();
			blog = (Blog) fullTextSession.get( Blog.class, blog.getId() );
			fullTextSession.delete( blog );
			blog = (Blog) fullTextSession.get( Article.class, article.getId() );
			fullTextSession.delete( blog );
			blog = (Blog) fullTextSession.get( TotalArticle.class, totalArticle.getId() );
			fullTextSession.delete( blog );
			tx.commit();
		}
		fullTextSession.close();
		super.tearDown();
	}

	@Test
	public void testBlogAndArticleAreNotIndexedInDraftStatus() throws Exception {
		Transaction tx = fullTextSession.beginTransaction();

		assertThat( getBlogEntriesFor( Blog.class ) ).as( "Blog is explicit intercepted" ).hasSize( 0 );
		assertThat( getBlogEntriesFor( Article.class ) ).as( "Article is inherently intercepted" ).hasSize( 0 );

		tx.commit();
	}

	@Test
	public void testTotalArticleIsIndexedInDraftStatus() throws Exception {
		Transaction tx = fullTextSession.beginTransaction();
		assertThat( getBlogEntriesFor( TotalArticle.class ) ).as( "TotalArticle is explicit not intercepted" )
				.hasSize( 1 );
		tx.commit();
	}


	@Test
	public void testBlogAndArticleAreIndexedInPublishedStatus() throws Exception {
		setAllBlogEntriesToStatus( BlogStatus.PUBLISHED );
		Transaction tx = fullTextSession.beginTransaction();

		assertThat( getBlogEntriesFor( Blog.class ) ).hasSize( 1 );
		assertThat( getBlogEntriesFor( Article.class ) ).as( "Article is inherently intercepted" ).hasSize( 1 );
		assertThat( getBlogEntriesFor( TotalArticle.class ) ).as( "TotalArticle is explicit not intercepted" )
				.hasSize( 1 );

		tx.commit();
	}


	@Test
	public void testBlogAndArticleAreNotIndexedInRemovedStatus() throws Exception {
		setAllBlogEntriesToStatus( BlogStatus.REMOVED );
		Transaction tx = fullTextSession.beginTransaction();

		assertThat( getBlogEntriesFor( Blog.class ) ).hasSize( 0 );
		assertThat( getBlogEntriesFor( Article.class ) ).as( "Article is inherently intercepted" ).hasSize( 0 );

		tx.commit();
	}

	@Test
	public void testTotalArticleIsIndexedInRemovedStatus() throws Exception {
		setAllBlogEntriesToStatus( BlogStatus.REMOVED );
		Transaction tx = fullTextSession.beginTransaction();

		assertThat( getBlogEntriesFor( TotalArticle.class ) ).as( "TotalArticle is explicit not intercepted" )
				.hasSize( 1 );

		tx.commit();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testInterceptorWithMassIndexer() throws Exception {
		setAllBlogEntriesToStatus( BlogStatus.PUBLISHED );

		List<Blog> allEntries = fullTextSession.createFullTextQuery( new MatchAllDocsQuery() ).list();
		assertEquals( "Wrong total number of entries", 3, allEntries.size() );
		for ( Blog blog : allEntries ) {
			assertTrue( blog.getStatus().equals( BlogStatus.PUBLISHED ) );
		}

		Transaction tx = fullTextSession.beginTransaction();

		fullTextSession.purgeAll( Blog.class );
		fullTextSession.purgeAll( Article.class );
		fullTextSession.purgeAll( TotalArticle.class );

		tx.commit();

		allEntries = fullTextSession.createFullTextQuery( new MatchAllDocsQuery() ).list();
		assertEquals( "Wrong total number of entries. Index should be empty after purge.", 0, allEntries.size() );

		tx = fullTextSession.beginTransaction();
		fullTextSession.createIndexer()
				.batchSizeToLoadObjects( 25 )
				.threadsToLoadObjects( 1 )
				.threadsForSubsequentFetching( 2 )
				.optimizeOnFinish( true )
				.startAndWait();
		tx.commit();

		allEntries = fullTextSession.createFullTextQuery( new MatchAllDocsQuery() ).list();
		assertEquals( "Wrong total number of entries.", 3, allEntries.size() );
	}

	private void createPersistAndIndexTestData() {
		blog = new Blog();
		blog.setTitle( "Hibernate Search now has soft deletes!" );
		blog.setStatus( BlogStatus.DRAFT );

		article = new Article();
		article.setTitle( "Hibernate Search: detailed description of soft deletes" );
		article.setStatus( BlogStatus.DRAFT );

		totalArticle = new TotalArticle();
		totalArticle.setTitle( "Hibernate Search: the total truth about soft deletes" );
		totalArticle.setStatus( BlogStatus.DRAFT );

		fullTextSession = Search.getFullTextSession( openSession() );

		Transaction tx = fullTextSession.beginTransaction();
		fullTextSession.persist( blog );
		fullTextSession.persist( article );
		fullTextSession.persist( totalArticle );
		tx.commit();

		fullTextSession.clear();
	}

	private void setAllBlogEntriesToStatus(BlogStatus status) {
		Transaction tx = fullTextSession.beginTransaction();

		blog = (Blog) fullTextSession.get( Blog.class, blog.getId() );
		blog.setStatus( status );

		article = (Article) fullTextSession.get( Article.class, article.getId() );
		article.setStatus( status );

		totalArticle = (TotalArticle) fullTextSession.get( TotalArticle.class, totalArticle.getId() );
		totalArticle.setStatus( status );

		tx.commit();
		fullTextSession.clear();
	}

	private List getBlogEntriesFor(Class<?> blogType) {
		TermQuery query = new TermQuery( new Term( ProjectionConstants.OBJECT_CLASS, blogType.getName() ) );
		return fullTextSession.createFullTextQuery( query ).list();
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
