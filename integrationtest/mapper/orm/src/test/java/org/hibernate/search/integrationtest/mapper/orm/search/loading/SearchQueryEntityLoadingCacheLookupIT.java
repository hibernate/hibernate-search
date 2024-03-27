/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.SharedCacheMode;

import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.singletype.SingleTypeLoadingMapping;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.singletype.SingleTypeLoadingModel;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.extension.ExpectedLog4jLog;
import org.hibernate.search.util.impl.test.extension.parameterized.ParameterizedPerClass;
import org.hibernate.search.util.impl.test.extension.parameterized.ParameterizedSetup;
import org.hibernate.stat.Statistics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.apache.logging.log4j.Level;

/**
 * Test cache lookup as part of entity loading when executing a search query
 * when only a single type is involved.
 */
@ParameterizedPerClass
public class SearchQueryEntityLoadingCacheLookupIT<T> extends AbstractSearchQueryEntityLoadingSingleTypeIT<T> {

	public static List<? extends Arguments> params() {
		List<Arguments> result = new ArrayList<>();
		forAllModelMappingCombinations( (model, mapping) -> {
			result.add( Arguments.of( model, mapping, null ) );
			for ( EntityLoadingCacheLookupStrategy strategy : EntityLoadingCacheLookupStrategy.values() ) {
				result.add( Arguments.of( model, mapping, strategy ) );
			}
		} );
		return result;
	}

	@RegisterExtension
	public static BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public static OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	@RegisterExtension
	public final ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	private EntityLoadingCacheLookupStrategy defaultCacheLookupStrategy;
	private SessionFactory sessionFactory;
	private SingleTypeLoadingModel<T> model;
	private SingleTypeLoadingMapping mapping;

	@Override
	protected BackendMock backendMock() {
		return backendMock;
	}

	@Override
	protected SessionFactory sessionFactory() {
		return sessionFactory;
	}

	@Override
	protected SingleTypeLoadingModel<T> model() {
		return model;
	}

	@Override
	protected SingleTypeLoadingMapping mapping() {
		return mapping;
	}

	@ParameterizedSetup
	@MethodSource("params")
	void setup(SingleTypeLoadingModel<T> model, SingleTypeLoadingMapping mapping,
			EntityLoadingCacheLookupStrategy defaultCacheLookupStrategy) {
		this.model = model;
		this.mapping = mapping;
		this.defaultCacheLookupStrategy = defaultCacheLookupStrategy;

		backendMock.expectAnySchema( model.getIndexName() );

		sessionFactory = ormSetupHelper.start().withProperty(
				HibernateOrmMapperSettings.QUERY_LOADING_CACHE_LOOKUP_STRATEGY,
				defaultCacheLookupStrategy
		)
				.withProperty( AvailableSettings.JAKARTA_SHARED_CACHE_MODE, SharedCacheMode.ALL.name() )
				.withConfiguration( c -> mapping.configure( c, model ) )
				.setup();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3349")
	void defaultStrategy() {
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
	void overriddenStrategy_skip() {
		testLoadingCacheLookupExpectingSkipCacheLookup(
				EntityLoadingCacheLookupStrategy.SKIP
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3349")
	void overriddenStrategy_persistenceContext() {
		testLoadingCacheLookupExpectingPersistenceContextOnlyLookup(
				EntityLoadingCacheLookupStrategy.PERSISTENCE_CONTEXT
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3349")
	void overriddenStrategy_2LC() {
		testLoadingCacheLookupExpectingSecondLevelCacheLookup(
				EntityLoadingCacheLookupStrategy.PERSISTENCE_CONTEXT_THEN_SECOND_LEVEL_CACHE
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3349")
	void overriddenStrategy_skip_fullCacheHits() {
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
	void overriddenStrategy_persistenceContext_fullCacheHits() {
		assumeTrue(
				mapping.isCacheLookupSupported(),
				"This test only makes sense if cache lookups are supported"
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
	void overriddenStrategy_2LC_fullCacheHits() {
		assumeTrue(
				mapping.isCacheLookupSupported(),
				"This test only makes sense if cache lookups are supported"
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
		if ( !mapping.isCacheLookupSupported() ) {
			logged.expectEvent( Level.DEBUG, "The entity loader for '" + model.getIndexedEntityName()
					+ "' is ignoring the cache lookup strategy" );
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
		if ( !mapping.isCacheLookupSupported() ) {
			logged.expectEvent( Level.DEBUG, "The entity loader for '" + model.getIndexedEntityName()
					+ "' is ignoring the cache lookup strategy" );
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

	@SuppressWarnings({ "unchecked", "deprecation" })
	private void testLoadingCacheLookup(EntityLoadingCacheLookupStrategy overriddenLookupStrategy,
			int entityCount,
			List<Integer> entitiesToPutInSecondLevelCache,
			List<Integer> entitiesToLoadInSession,
			int expectedSecondLevelCacheHitCount,
			int expectedPersistenceContextHitCount,
			boolean expectStatementExecution) {
		Statistics statistics = sessionFactory.getStatistics();
		statistics.setStatisticsEnabled( true );
		statistics.clear();
		persistThatManyEntities( entityCount );
		assertThat( statistics.getSecondLevelCachePutCount() )
				.as( "Test setup sanity check" )
				.isEqualTo( entityCount );

		// Remove some entities from the second level cache
		for ( int i = 0; i < entityCount; i++ ) {
			if ( !entitiesToPutInSecondLevelCache.contains( i ) ) {
				sessionFactory.getCache().evict( model.getIndexedClass(), i );
			}
		}

		testLoadingThatManyEntities(
				session -> {
					// Pre-load some entities into the session
					for ( Integer id : entitiesToLoadInSession ) {
						Hibernate.initialize( session.getReference( model.getIndexedClass(), id ) );
					}
					assertThat( session.unwrap( SessionImplementor.class ).getPersistenceContext().getEntitiesByKey() )
							.as( "Test setup sanity check" )
							.hasSize( entitiesToLoadInSession.size() );
				},
				f -> {
					if ( overriddenLookupStrategy != null ) {
						f.cacheLookupStrategy( overriddenLookupStrategy );
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
