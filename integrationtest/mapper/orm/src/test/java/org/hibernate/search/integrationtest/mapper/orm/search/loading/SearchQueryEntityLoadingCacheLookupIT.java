/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.SharedCacheMode;

import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.rule.ExpectedLog4jLog;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Test cache lookup as part of entity loading when executing a search query
 * when only a single type is involved.
 */
@RunWith(Parameterized.class)
public class SearchQueryEntityLoadingCacheLookupIT<T> extends AbstractSearchQueryEntityLoadingSingleTypeIT<T> {

	@Parameterized.Parameters(name = "Default strategy: {1} - {0}")
	public static List<Object[]> data() {
		List<Object[]> result = new ArrayList<>();
		for ( SingleTypeLoadingModelPrimitives<?> primitives : allSingleTypeLoadingModelPrimitives() ) {
			result.add( new Object[] { primitives, null } );
			for ( EntityLoadingCacheLookupStrategy strategy : EntityLoadingCacheLookupStrategy.values() ) {
				result.add( new Object[] { primitives, strategy } );
			}
		}
		return result;
	}

	@Rule
	public final ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	private final EntityLoadingCacheLookupStrategy defaultCacheLookupStrategy;

	private SessionFactory sessionFactory;

	public SearchQueryEntityLoadingCacheLookupIT(SingleTypeLoadingModelPrimitives<T> primitives,
			EntityLoadingCacheLookupStrategy defaultCacheLookupStrategy) {
		super( primitives );
		this.defaultCacheLookupStrategy = defaultCacheLookupStrategy;
	}

	@Before
	public void setup() {
		backendMock.expectAnySchema( primitives.getIndexName() );

		sessionFactory = ormSetupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.QUERY_LOADING_CACHE_LOOKUP_STRATEGY,
						defaultCacheLookupStrategy
				)
				.withProperty( AvailableSettings.JPA_SHARED_CACHE_MODE, SharedCacheMode.ALL.name() )
				.setup( primitives.getIndexedClass() );

		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3349")
	public void defaultStrategy() {
		if ( defaultCacheLookupStrategy == null ) {
			testLoadingCacheLookupExpectingSkipCacheLookup( null );
		}
		else {
			switch ( defaultCacheLookupStrategy ) {
				case SKIP:
					testLoadingCacheLookupExpectingSkipCacheLookup( null );
					break;
				case PERSISTENCE_CONTEXT:
					testLoadingCacheLookupExpectingPersistenceContextOnlyLookup( null );
					break;
				case PERSISTENCE_CONTEXT_THEN_SECOND_LEVEL_CACHE:
					testLoadingCacheLookupExpectingSecondLevelCacheLookup( null );
					break;
			}
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3349")
	public void overriddenStrategy_skip() {
		testLoadingCacheLookupExpectingSkipCacheLookup(
				EntityLoadingCacheLookupStrategy.SKIP
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3349")
	public void overriddenStrategy_persistenceContext() {
		testLoadingCacheLookupExpectingPersistenceContextOnlyLookup(
				EntityLoadingCacheLookupStrategy.PERSISTENCE_CONTEXT
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3349")
	public void overriddenStrategy_2LC() {
		testLoadingCacheLookupExpectingSecondLevelCacheLookup(
				EntityLoadingCacheLookupStrategy.PERSISTENCE_CONTEXT_THEN_SECOND_LEVEL_CACHE
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3349")
	public void overriddenStrategy_skip_fullCacheHits() {
		testLoadingCacheLookup(
				EntityLoadingCacheLookupStrategy.SKIP,
				// Persist that many entities
				10,
				// Add some of them the second level cache
				Arrays.asList( 0, 1 ),
				// Add all of them to the session when searching
				Arrays.asList( 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ),
				// Expect no successful 2LC lookups (2LC lookup is disabled)
				0,
				// Expect successful PC lookups for all entities (they happen after the DB statement)
				10,
				// Still expect a DB statement since the PC lookups happen after the DB statement
				true
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3349")
	public void overriddenStrategy_persistenceContext_fullCacheHits() {
		Assume.assumeTrue(
				"This test only makes sense if cache lookups are supported",
				primitives.isCacheLookupSupported()
		);

		testLoadingCacheLookup(
				EntityLoadingCacheLookupStrategy.PERSISTENCE_CONTEXT,
				// Persist that many entities
				10,
				// Add some of them the second level cache
				Arrays.asList( 0, 1 ),
				// Add all of them to the session when searching
				Arrays.asList( 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ),
				// Expect no successful 2LC lookups (2LC lookup is disabled)
				0,
				// Expect successful PC lookups for all entities
				10,
				// Expect no DB statement since everything has been loaded
				false
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3349")
	public void overriddenStrategy_2LC_fullCacheHits() {
		Assume.assumeTrue(
				"This test only makes sense if cache lookups are supported",
				primitives.isCacheLookupSupported()
		);

		testLoadingCacheLookup(
				EntityLoadingCacheLookupStrategy.PERSISTENCE_CONTEXT_THEN_SECOND_LEVEL_CACHE,
				// Persist that many entities
				10,
				// Add half of them the second level cache
				Arrays.asList( 0, 1, 2, 3, 4 ),
				// Add the others to the session when searching
				Arrays.asList( 5, 6, 7, 8, 9 ),
				// Expect a few successful 2LC lookups
				5,
				// Expect a few successful PC lookups
				5,
				// Expect no DB statement since everything has been loaded
				false
		);
	}

	@Override
	protected SessionFactory sessionFactory() {
		return sessionFactory;
	}

	private void testLoadingCacheLookupExpectingSkipCacheLookup(
			EntityLoadingCacheLookupStrategy overriddenLookupStrategy) {
		testLoadingCacheLookup(
				overriddenLookupStrategy,
				// Persist that many entities
				10,
				// Add these to the second level cache
				Arrays.asList( 0, 1 ),
				// Add these to the session when searching
				Arrays.asList( 2, 3 ),
				// Expect no successful 2LC lookups (2LC is disabled)
				0,
				// Expect a few successful PC lookups (they happen after the DB statement)
				2,
				// Expect a DB statement to load the entities
				true
		);
	}

	private void testLoadingCacheLookupExpectingPersistenceContextOnlyLookup(
			EntityLoadingCacheLookupStrategy overriddenLookupStrategy) {
		if ( !primitives.isCacheLookupSupported() ) {
			logged.expectMessage(
					"The entity loader for '" + primitives.getIndexedClass().getName()
					+ "' will ignore the cache lookup strategy"
			);
			testLoadingCacheLookupExpectingSkipCacheLookup( overriddenLookupStrategy );
			return;
		}

		testLoadingCacheLookup(
				overriddenLookupStrategy,
				// Persist that many entities
				10,
				// Add these to the second level cache
				Arrays.asList( 0, 1 ),
				// Add these to the session when searching
				Arrays.asList( 2, 3 ),
				// Expect no successful 2LC lookups (2LC lookup is disabled)
				0,
				// Expect a few successful PC lookups
				2,
				// Expect a DB statement to load the rest
				true
		);
	}

	private void testLoadingCacheLookupExpectingSecondLevelCacheLookup(
			EntityLoadingCacheLookupStrategy overriddenLookupStrategy) {
		if ( !primitives.isCacheLookupSupported() ) {
			logged.expectMessage(
					"The entity loader for '" + primitives.getIndexedClass().getName()
					+ "' will ignore the cache lookup strategy"
			);
			testLoadingCacheLookupExpectingSkipCacheLookup( overriddenLookupStrategy );
			return;
		}

		testLoadingCacheLookup(
				overriddenLookupStrategy,
				// Persist that many entities
				10,
				// Add these to the second level cache
				Arrays.asList( 0, 1 ),
				// Add these to the session when searching
				Arrays.asList( 2, 3 ),
				// Expect a few successful 2LC lookups
				2,
				// Expect a few successful PC lookups
				2,
				// Expect a DB statement to load the rest
				true
		);
	}

	private void testLoadingCacheLookup(EntityLoadingCacheLookupStrategy overriddenLookupStrategy,
			int entityCount,
			List<Integer> entitiesToPutInSecondLevelCache,
			List<Integer> entitiesToLoadInSession,
			int expectedSecondLevelCacheHitCount,
			int expectedPersistenceContextHitCount,
			boolean expectStatementExecution) {
		sessionFactory.getStatistics().setStatisticsEnabled( true );
		sessionFactory.getStatistics().clear();
		persistThatManyEntities( entityCount );
		assertThat( sessionFactory.getStatistics().getSecondLevelCachePutCount() )
				.as( "Test setup sanity check" )
				.isEqualTo( entityCount );

		// Remove some entities from the second level cache
		for ( int i = 0; i < entityCount; i++ ) {
			if ( !entitiesToPutInSecondLevelCache.contains( i ) ) {
				sessionFactory.getCache().evict( primitives.getIndexedClass(), i );
			}
		}

		testLoadingThatManyEntities(
				session -> {
					// Pre-load some entities into the session
					for ( Integer id : entitiesToLoadInSession ) {
						Hibernate.initialize( session.getReference( primitives.getIndexedClass(), id ) );
					}
					assertThat( session.unwrap( SessionImplementor.class ).getPersistenceContext().getEntitiesByKey() )
							.as( "Test setup sanity check" )
							.hasSize( entitiesToLoadInSession.size() );
				},
				loadingOptions -> {
					if ( overriddenLookupStrategy != null ) {
						return loadingOptions.cacheLookupStrategy( overriddenLookupStrategy );
					}
					else {
						return loadingOptions;
					}
				},
				entityCount,
				c -> {
					c.assertEntityLoadCount()
							.isEqualTo( entityCount - expectedPersistenceContextHitCount - expectedSecondLevelCacheHitCount );
					c.assertSecondLevelCacheHitCount()
							.isEqualTo( expectedSecondLevelCacheHitCount );
					c.assertStatementExecutionCount()
							.isEqualTo( expectStatementExecution ? 1 : 0 );
				}
		);
	}

}
