/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.filter;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeFilter;
import org.apache.lucene.search.QueryCachingPolicy;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermQuery;
import org.hibernate.Session;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.filter.Employee.Role;
import org.hibernate.search.test.filter.FieldConstraintFilterFactoryWithoutKeyMethod.BuildFilterInvocation;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.ElasticsearchSupportInProgress;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class FullTextFilterTest extends SearchTestBase {
	private QueryCachingPolicy cachingPolicy;
	private BooleanQuery query;
	private FullTextSession fullTextSession;

	@Test
	public void testNamedFilters() {
		FullTextQuery ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		assertEquals( "No filter should happen", 3, ftQuery.getResultSize() );

		ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		ftQuery.disableFullTextFilter( "bestDriver" ); //was not enabled, but should be harmless
		ftQuery.enableFullTextFilter( "bestDriver" );
		assertEquals( "Should filter out Gavin", 2, ftQuery.getResultSize() );

		ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		ftQuery.enableFullTextFilter( "bestDriver" );
		ftQuery.enableFullTextFilter( "security" ).setParameter( "login", "andre" );
		assertEquals( "Should filter to limit to Emmanuel", 1, ftQuery.getResultSize() );

		ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		ftQuery.enableFullTextFilter( "bestDriver" );
		ftQuery.enableFullTextFilter( "security" ).setParameter( "login", "andre" );
		ftQuery.disableFullTextFilter( "security" );
		ftQuery.disableFullTextFilter( "bestDriver" );
		assertEquals( "Should not filter anymore", 3, ftQuery.getResultSize() );
	}

	@Test
	@Category(ElasticsearchSupportInProgress.class) // HSEARCH-2405 Support caching for filters with Elasticsearch
	// Moreover the Elasticsearch backend does not support custom Lucene filters.
	public void testCache() {
		FullTextQuery ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		assertEquals( "No filter should happen", 3, ftQuery.getResultSize() );

		ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		ftQuery.enableFullTextFilter( "cacheresultstest" );
		assertEquals( "Should filter out all", 0, ftQuery.getResultSize() );

		// HSEARCH-174 - we call System.gc() to force a garbage collection.
		// Prior to the fix for HSEARCH-174 this would cause the filter to be
		// garbage collected since Lucene used weak references.
		System.gc();

		ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		ftQuery.enableFullTextFilter( "cacheresultstest" );
		try {
			ftQuery.getResultSize();
		}
		catch (IllegalStateException e) {
			fail( "Cache results does not work" );
		}

		ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		ftQuery.enableFullTextFilter( "cacheinstancetest" );
		InstanceBasedExcludeAllFilterFactory.assertInstancesCreated( 0 );
		assertEquals( "Should filter out all", 0, ftQuery.getResultSize() );
		InstanceBasedExcludeAllFilterFactory.assertInstancesCreated( 1 );

		ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		ftQuery.enableFullTextFilter( "cacheinstancetest" );
		ftQuery.getResultSize();
		InstanceBasedExcludeAllFilterFactory.assertInstancesCreated( 1 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-295")
	@Category(ElasticsearchSupportInProgress.class) // HSEARCH-2405 Support caching for filters with Elasticsearch
	// Moreover the Elasticsearch backend does not support custom Lucene filters.
	public void testFiltersCreatedByFactoryWithoutKeyMethodShouldBeCachedByAllParameterNamesAndValues() {
		assertEquals( 0, FieldConstraintFilterFactoryWithoutKeyMethod.getBuiltFilters().size() );

		FullTextQuery ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		assertEquals( "No filter should happen", 3, ftQuery.getResultSize() );

		// 1. Creating one filter
		ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		ftQuery.enableFullTextFilter( "cacheinstancefromfactorywithoutkeymethodtest" )
			.setParameter( "field", "teacher" )
			.setParameter( "value", "andre" );

		assertEquals( 1, ftQuery.getResultSize() );
		assertThat( FieldConstraintFilterFactoryWithoutKeyMethod.getBuiltFilters() ).containsExactly( new BuildFilterInvocation( "teacher", "andre" ) );

		// 2. Creating another filter with other param value
		ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		ftQuery.enableFullTextFilter( "cacheinstancefromfactorywithoutkeymethodtest" )
			.setParameter( "field", "teacher" )
			.setParameter( "value", "max" );

		assertEquals( 1, ftQuery.getResultSize() );
		assertThat( FieldConstraintFilterFactoryWithoutKeyMethod.getBuiltFilters() ).containsExactly(
				new BuildFilterInvocation( "teacher", "andre" ),
				new BuildFilterInvocation( "teacher", "max" )
		);

		// 3. Creating the first filter again, should be obtained from cache
		ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		ftQuery.enableFullTextFilter( "cacheinstancefromfactorywithoutkeymethodtest" )
			.setParameter( "field", "teacher" )
			.setParameter( "value", "andre" );

		assertEquals( 1, ftQuery.getResultSize() );
		assertThat( FieldConstraintFilterFactoryWithoutKeyMethod.getBuiltFilters() ).containsExactly(
				new BuildFilterInvocation( "teacher", "andre" ),
				new BuildFilterInvocation( "teacher", "max" )
		);

		// 4. Creating the first filter again, just using different parameter order, should be obtained from cache
		ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		ftQuery.enableFullTextFilter( "cacheinstancefromfactorywithoutkeymethodtest" )
			.setParameter( "value", "andre" )
			.setParameter( "field", "teacher" );

		assertEquals( 1, ftQuery.getResultSize() );
		assertThat( FieldConstraintFilterFactoryWithoutKeyMethod.getBuiltFilters() ).containsExactly(
				new BuildFilterInvocation( "teacher", "andre" ),
				new BuildFilterInvocation( "teacher", "max" )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-295")
	@Category(SkipOnElasticsearch.class)
	// The Elasticsearch backend does not support custom Lucene filters.
	public void testFiltersWithoutKeyMethodShouldBeCachedByAllParameterNamesAndValues() {
		// Discarding all instantiations stemming from SF bootstrap
		FieldConstraintFilterWithoutKeyMethod.getInstances().clear();

		FullTextQuery ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		assertEquals( "No filter should happen", 3, ftQuery.getResultSize() );

		// 1. Creating one filter
		ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		ftQuery.enableFullTextFilter( "cacheinstancewithoutkeymethodtest" )
			.setParameter( "field", "teacher" )
			.setParameter( "value", "andre" );

		assertEquals( 1, ftQuery.getResultSize() );
		assertThat( FieldConstraintFilterWithoutKeyMethod.getInstances() ).containsExactly( new FieldConstraintFilterWithoutKeyMethod( "teacher", "andre" ) );

		// 2. Creating another filter with other param value
		ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		ftQuery.enableFullTextFilter( "cacheinstancewithoutkeymethodtest" )
			.setParameter( "field", "teacher" )
			.setParameter( "value", "max" );

		assertEquals( 1, ftQuery.getResultSize() );
		assertThat( FieldConstraintFilterWithoutKeyMethod.getInstances() ).containsExactly(
				new FieldConstraintFilterWithoutKeyMethod( "teacher", "andre" ),
				new FieldConstraintFilterWithoutKeyMethod( "teacher", "max" )
		);

		// 3. Creating the first filter again, should be obtained from cache
		ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		ftQuery.enableFullTextFilter( "cacheinstancewithoutkeymethodtest" )
			.setParameter( "field", "teacher" )
			.setParameter( "value", "andre" );

		assertEquals( 1, ftQuery.getResultSize() );
		assertThat( FieldConstraintFilterWithoutKeyMethod.getInstances() ).containsExactly(
				new FieldConstraintFilterWithoutKeyMethod( "teacher", "andre" ),
				new FieldConstraintFilterWithoutKeyMethod( "teacher", "max" )
		);

		// 4. Creating the first filter again, just using different parameter order, should be obtained from cache
		ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		ftQuery.enableFullTextFilter( "cacheinstancewithoutkeymethodtest" )
			.setParameter( "value", "andre" )
			.setParameter( "field", "teacher" );

		assertEquals( 1, ftQuery.getResultSize() );
		assertThat( FieldConstraintFilterWithoutKeyMethod.getInstances() ).containsExactly(
				new FieldConstraintFilterWithoutKeyMethod( "teacher", "andre" ),
				new FieldConstraintFilterWithoutKeyMethod( "teacher", "max" )
		);
	}

	@Test
	public void testStraightFilters() {
		FullTextQuery ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		ftQuery.enableFullTextFilter( "bestDriver" );
		TermQuery termQuery = new TermQuery( new Term( "name", "liz" ) );
		Filter termFilter = new QueryWrapperFilter( termQuery );
		ftQuery.setFilter( termFilter );
		assertEquals( "Should select only liz", 1, ftQuery.getResultSize() );

		ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		ftQuery.setFilter( termFilter );
		ftQuery.enableFullTextFilter( "bestDriver" );
		ftQuery.enableFullTextFilter( "security" ).setParameter( "login", "andre" );
		ftQuery.disableFullTextFilter( "security" );
		ftQuery.disableFullTextFilter( "bestDriver" );
		ftQuery.setFilter( null );
		assertEquals( "Should not filter anymore", 3, ftQuery.getResultSize() );
	}

	@Test
	@Category(SkipOnElasticsearch.class)
	// The Elasticsearch backend does not support custom Lucene filters.
	public void testEmptyFilters() {
		FullTextQuery ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		ftQuery.enableFullTextFilter( "bestDriver" );
		TermQuery termQuery = new TermQuery( new Term( "name", "liz" ) );
		Filter termFilter = new QueryWrapperFilter( termQuery );
		ftQuery.setFilter( termFilter );
		assertEquals( "Should select only liz", 1, ftQuery.getResultSize() );

		ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		ftQuery.enableFullTextFilter( "bestDriver" );
		ftQuery.enableFullTextFilter( "emptyWithDeprecatedFilterType" );
		assertEquals( "two filters, one is empty, should not match anything", 0, ftQuery.getResultSize() );
	}

	@TestForIssue(jiraKey = "HSEARCH-1513")
	@Test
	@Category(SkipOnElasticsearch.class)
	// The Elasticsearch backend does not support custom Lucene filters.
	public void testCachedEmptyFilters() {
		FullTextQuery ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		ftQuery.enableFullTextFilter( "bestDriver" );
		Calendar calendar = GregorianCalendar.getInstance( TimeZone.getTimeZone( "GMT" ), Locale.ROOT );
		calendar.set( Calendar.YEAR, 2001 );
		long from = DateTools.round( calendar.getTime().getTime(), DateTools.Resolution.YEAR );
		calendar.set( Calendar.YEAR, 2005 );
		long to = DateTools.round( calendar.getTime().getTime(), DateTools.Resolution.YEAR );
		Filter dateFilter = NumericRangeFilter.newLongRange( "delivery", from, to, true, true );
		ftQuery.setFilter( dateFilter );
		assertEquals( "Should select only liz", 1, ftQuery.getResultSize() );

		ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		ftQuery.enableFullTextFilter( "bestDriver" );
		ftQuery.enableFullTextFilter( "cached_empty" );
		assertEquals( "two filters, one is empty, should not match anything", 0, ftQuery.getResultSize() );
	}

	@Test
	public void testMultipleFiltersOfSameTypeWithDifferentParameters() {
		FullTextQuery ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		ftQuery.enableFullTextFilter( "fieldConstraintFilter-1" )
				.setParameter( "field", "teacher" )
				.setParameter( "value", "andre" );
		ftQuery.enableFullTextFilter( "fieldConstraintFilter-2" )
				.setParameter( "field", "teacher" )
				.setParameter( "value", "aaron" );
		assertEquals( "Should apply both filters resulting in 0 results", 0, ftQuery.getResultSize() );
	}

	@Test
	public void testFilterDefinedOnSuperClass() {
		TermQuery query = new TermQuery( new Term( "employer", "Red Hat" ) );
		FullTextQuery ftQuery = fullTextSession.createFullTextQuery( query, Employee.class );
		ftQuery.enableFullTextFilter( "roleFilter" )
				.setParameter( "role", Role.ADMINISTRATOR );

		assertEquals( "Should find the filter defined in the super class", 1, ftQuery.getResultSize() );
	}

	@Test
	public void testUnknownFilterNameThrowsException() {
		FullTextQuery ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		try {
			ftQuery.enableFullTextFilter( "foo" );
			fail( "Retrieving an unknown filter should throw a SearchException" );
		}
		catch (SearchException e) {
			assertEquals( "Wrong message", "HSEARCH000115: Unknown @FullTextFilter: 'foo'", e.getMessage() );
		}
	}

	private void createData() {
		try ( Session s = openSession() ) {
			s.getTransaction().begin();
			Calendar cal = GregorianCalendar.getInstance( TimeZone.getTimeZone( "GMT" ), Locale.ROOT );
			cal.set( 2006, 10, 11 );
			Driver driver = new Driver();
			driver.setDelivery( cal.getTime() );
			driver.setId( 1 );
			driver.setName( "Emmanuel" );
			driver.setScore( 5 );
			driver.setTeacher( "andre" );
			s.persist( driver );

			cal.set( 2007, 10, 11 );
			driver = new Driver();
			driver.setDelivery( cal.getTime() );
			driver.setId( 2 );
			driver.setName( "Gavin" );
			driver.setScore( 3 );
			driver.setTeacher( "aaron" );
			s.persist( driver );

			cal.set( 2004, 10, 11 );
			driver = new Driver();
			driver.setDelivery( cal.getTime() );
			driver.setId( 3 );
			driver.setName( "Liz" );
			driver.setScore( 5 );
			driver.setTeacher( "max" );
			s.persist( driver );

			String employer = "Red Hat";
			Employee employee = new FullTimeEmployee();
			employee.setId( 1 );
			employee.setFullName( "John D Doe" );
			employee.setRole( Role.ADMINISTRATOR );
			employee.setEmployer( employer );
			s.persist( employee );

			employee = new FullTimeEmployee();
			employee.setId( 2 );
			employee.setFullName( "Mary S. Doe" );
			employee.setRole( Role.DEVELOPER );
			employee.setEmployer( employer );
			s.persist( employee );

			employee = new PartTimeEmployee();
			employee.setId( 3 );
			employee.setFullName( "Dave Connor" );
			employee.setRole( Role.CONSULTANT );
			employee.setEmployer( employer );
			s.persist( employee );

			s.getTransaction().commit();
		}
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		cachingPolicy = IndexSearcher.getDefaultQueryCachingPolicy();
		IndexSearcher.setDefaultQueryCachingPolicy( QueryCachingPolicy.ALWAYS_CACHE );
		createData();
		query = createQuery();
		fullTextSession = Search.getFullTextSession( openSession() );
		fullTextSession.getTransaction().begin();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		try {
			fullTextSession.getTransaction().commit();
			fullTextSession.close();
		}
		finally {
			// Reset the caching policy to its original state
			if ( cachingPolicy != null ) {
				IndexSearcher.setDefaultQueryCachingPolicy( cachingPolicy );
			}
		}
		super.tearDown();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Driver.class,
				Soap.class,
				FullTimeEmployee.class,
				PartTimeEmployee.class
		};
	}

	@Override
	public void configure(Map<String,Object> cfg) {
		cfg.put( "hibernate.search.filter.cache_docidresults.size", "10" );
		InstanceBasedExcludeAllFilterFactory.reset();
	}

	private BooleanQuery createQuery() {
		return new BooleanQuery.Builder()
				.add( new TermQuery( new Term( "teacher", "andre" ) ), BooleanClause.Occur.SHOULD )
				.add( new TermQuery( new Term( "teacher", "max" ) ), BooleanClause.Occur.SHOULD )
				.add( new TermQuery( new Term( "teacher", "aaron" ) ), BooleanClause.Occur.SHOULD )
				.build();
	}
}
