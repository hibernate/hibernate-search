/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.sorting;

import java.util.List;
import java.util.Map;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.Transaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.query.Book;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test for sorting on fields which are not added as doc value fields and thus require index uninverting.
 *
 * @author Gunnar Morling
 */
public class SortWithIndexUninvertingTest extends SearchTestBase {

	private static FullTextSession fullTextSession;
	private static QueryParser queryParser;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		fullTextSession = Search.getFullTextSession( openSession() );
		queryParser = new QueryParser(
				"title",
				TestConstants.stopAnalyzer
		);

		createTestContractors();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		// check for ongoing transaction which is an indicator that something went wrong
		// don't call the cleanup methods in this case. Otherwise the original error get swallowed
		if ( fullTextSession.getTransaction().getStatus() != TransactionStatus.ACTIVE ) {
			deleteTestContractors();
			fullTextSession.close();
		}
		super.tearDown();
	}


	@Test
	public void testCombinedQueryOnIndexWithSortFieldAndIndexToBeUninverted() throws Exception {
		Transaction tx = fullTextSession.beginTransaction();

		Query query = queryParser.parse( "name:Bill" );
		FullTextQuery hibQuery = fullTextSession.createFullTextQuery( query, Plumber.class, BrickLayer.class );
		Sort sort = new Sort( new SortField( "sortName", SortField.Type.STRING ) ); //ASC
		hibQuery.setSort( sort );

		@SuppressWarnings("unchecked")
		List<Book> result = hibQuery.list();
		assertNotNull( result );
		assertThat( result ).onProperty( "name" )
			.describedAs( "Expecting results from index with sort field and uninverted index in the correct sort order" )
			.containsExactly( "Bill the brick layer", "Bill the plumber" );

		tx.commit();
	}

	/**
	 * The index is shared by two entities. One declares the required sorts, the other does not. As this would require
	 * uninverting the index for one entity but not the other, that situation is considered inconsistent and an
	 * exception is expected.
	 */
	@Test
	@Category(SkipOnElasticsearch.class) // This problem does not affect the Elasticsearch backend
	public void testQueryOnIndexSharedByEntityWithRequiredSortFieldAndEntityWithoutRaisesException() throws Exception {
		Transaction tx = fullTextSession.beginTransaction();

		Query query = queryParser.parse( "name:Bill" );
		FullTextQuery hibQuery = fullTextSession.createFullTextQuery( query, Thatcher.class, BrickLayer.class );
		Sort sort = new Sort( new SortField( "sortName", SortField.Type.STRING ) ); //ASC
		hibQuery.setSort( sort );

		try {
			hibQuery.list();
			fail( "Expected exception was not raised" );
		}
		catch (Exception e) {
			assertThat( e.getMessage() ).contains( "HSEARCH000298" );
		}

		tx.commit();
	}

	private void createTestContractors() {
		Transaction tx = fullTextSession.beginTransaction();

		fullTextSession.save( new Plumber( 1, "Bill the plumber" ) );
		fullTextSession.save( new BrickLayer( 2, "Bill the brick layer", "Johnson" ) );
		fullTextSession.save( new BrickLayer( 4, "Barny the brick layer", "Johnson" ) );
		fullTextSession.save( new BrickLayer( 5, "Bart the brick layer", "Higgins" ) );
		fullTextSession.save( new BrickLayer( 6, "Barny the brick layer", "Higgins" ) );
		fullTextSession.save( new Thatcher( 3, "Bill the thatcher" ) );

		tx.commit();
		fullTextSession.clear();
	}

	private void deleteTestContractors() {
		Transaction tx = fullTextSession.beginTransaction();
		fullTextSession.createQuery( "delete " + Plumber.class.getName() ).executeUpdate();
		fullTextSession.createQuery( "delete " + BrickLayer.class.getName() ).executeUpdate();
		fullTextSession.createQuery( "delete " + Thatcher.class.getName() ).executeUpdate();
		tx.commit();
		fullTextSession.clear();
	}

	@Override
	public void configure(Map<String, Object> settings) {
		settings.put( Environment.INDEX_UNINVERTING_ALLOWED, "true" );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Plumber.class,
				BrickLayer.class,
				Thatcher.class
		};
	}
}
