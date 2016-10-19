/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.interceptor;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
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
		if ( getTransactionStatus( fullTextSession ) != TransactionStatus.ACTIVE ) {
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

	private static TransactionStatus getTransactionStatus(FullTextSession fullTextSession) {
		SharedSessionContractImplementor actualSession = (SharedSessionContractImplementor) fullTextSession;
		return actualSession.accessTransaction().getStatus();
	}

	@Test
	public void testBlogAndArticleAreNotIndexedInDraftStatus() throws Exception {
		Transaction tx = fullTextSession.beginTransaction();

		List<Blog> blogEntries = getBlogEntries();
		assertThat( blogEntries ).as( "Blog is explicitly intercepted" ).excludes( blog );
		assertThat( blogEntries ).as( "Article is inherently intercepted" ).excludes( article );

		tx.commit();
	}

	@Test
	public void testTotalArticleIsIndexedInDraftStatus() throws Exception {
		Transaction tx = fullTextSession.beginTransaction();
		List<Blog> blogEntries = getBlogEntries();
		assertThat( blogEntries ).as( "TotalArticle is explicitly not intercepted" ).contains( totalArticle );
		tx.commit();
	}


	@Test
	public void testBlogAndArticleAreIndexedInPublishedStatus() throws Exception {
		setAllBlogEntriesToStatus( BlogStatus.PUBLISHED );
		Transaction tx = fullTextSession.beginTransaction();

		List<Blog> blogEntries = getBlogEntries();
		assertThat( blogEntries ).contains( blog );
		assertThat( blogEntries ).as( "Article is inherently intercepted" ).contains( article );
		assertThat( blogEntries ).as( "TotalArticle is explicitly not intercepted" ).contains( totalArticle );

		tx.commit();
	}


	@Test
	public void testBlogAndArticleAreNotIndexedInRemovedStatus() throws Exception {
		setAllBlogEntriesToStatus( BlogStatus.REMOVED );
		Transaction tx = fullTextSession.beginTransaction();

		List<Blog> blogEntries = getBlogEntries();
		assertThat( blogEntries ).excludes( blog );
		assertThat( blogEntries ).as( "Article is inherently intercepted" ).excludes( article );

		tx.commit();
	}

	@Test
	public void testTotalArticleIsIndexedInRemovedStatus() throws Exception {
		setAllBlogEntriesToStatus( BlogStatus.REMOVED );
		Transaction tx = fullTextSession.beginTransaction();

		List<Blog> blogEntries = getBlogEntries();
		assertThat( blogEntries ).as( "TotalArticle is explicitly not intercepted" ).contains( totalArticle );

		tx.commit();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testInterceptorWithMassIndexer() throws Exception {
		setAllBlogEntriesToStatus( BlogStatus.PUBLISHED );

		List<Blog> blogEntries = getBlogEntries();
		assertEquals( "Wrong total number of entries", 3, blogEntries.size() );
		for ( Blog blog : blogEntries ) {
			assertTrue( blog.getStatus().equals( BlogStatus.PUBLISHED ) );
		}

		Transaction tx = fullTextSession.beginTransaction();

		fullTextSession.purgeAll( Blog.class );
		fullTextSession.purgeAll( Article.class );
		fullTextSession.purgeAll( TotalArticle.class );

		tx.commit();

		blogEntries = fullTextSession.createFullTextQuery( new MatchAllDocsQuery() ).list();
		assertEquals( "Wrong total number of entries. Index should be empty after purge.", 0, blogEntries.size() );

		tx = fullTextSession.beginTransaction();
		fullTextSession.createIndexer()
				.batchSizeToLoadObjects( 25 )
				.threadsToLoadObjects( 1 )
				.threadsForSubsequentFetching( 2 )
				.optimizeOnFinish( true )
				.startAndWait();
		tx.commit();

		blogEntries = getBlogEntries();
		assertEquals( "Wrong total number of entries.", 3, blogEntries.size() );
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

	@SuppressWarnings("unchecked")
	private List<Blog> getBlogEntries() {
		Query query = new MatchAllDocsQuery();
		return fullTextSession.createFullTextQuery( query, Blog.class ).list();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Blog.class,
				Article.class,
				TotalArticle.class
		};
	}
}
