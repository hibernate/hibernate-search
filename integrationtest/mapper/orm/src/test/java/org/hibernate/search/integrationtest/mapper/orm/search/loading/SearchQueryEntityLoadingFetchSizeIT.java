/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.singletype.SingleTypeLoadingMapping;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.singletype.SingleTypeLoadingModel;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test fetch size of entity loading when executing a search query
 * when only a single type is involved.
 */
class SearchQueryEntityLoadingFetchSizeIT<T> extends AbstractSearchQueryEntityLoadingSingleTypeIT<T> {

	SearchQueryEntityLoadingFetchSizeIT() {
		super( null, null );
	}

	public static List<? extends Arguments> params() {
		List<Arguments> result = new ArrayList<>();
		forAllModelMappingCombinations( (model, mapping) -> {
			result.add( Arguments.of( model, mapping ) );
		} );
		return result;
	}

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	@Override
	protected BackendMock backendMock() {
		return backendMock;
	}

	@Override
	protected SessionFactory sessionFactory() {
		return sessionFactory;
	}

	@ParameterizedTest(name = "{0}, {1}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-3349")
	void defaults(SingleTypeLoadingModel<T> model, SingleTypeLoadingMapping mapping) {
		init( model, mapping );
		testLoadingFetchSize(
				// Do not configure search.loading.fetch_size
				null,
				// Do not override fetch size at query level
				null,
				// Persist that many entities
				150,
				// 100 entities to load with a (default) fetch size of 100 => 2 fetches are necessary
				2
		);
	}

	@ParameterizedTest(name = "{0}, {1}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-3349")
	void configurationProperty(SingleTypeLoadingModel<T> model, SingleTypeLoadingMapping mapping) {
		init( model, mapping );
		testLoadingFetchSize(
				// Configure search.loading.fetch_size with this value
				50,
				// Do not override fetch size at query level
				null,
				// Persist that many entities
				100,
				// 100 entities to load with a fetch size of 50 => 2 fetches are necessary
				2
		);
	}


	@ParameterizedTest(name = "{0}, {1}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-3349")
	void override_valid(SingleTypeLoadingModel<T> model, SingleTypeLoadingMapping mapping) {
		init( model, mapping );
		testLoadingFetchSize(
				// Configure search.loading.fetch_size with this value (will be ignored)
				100,
				// Override fetch size at query level with this value
				20,
				// Persist that many entities
				100,
				// 100 entities to load with a fetch size of 20 => 5 fetches are necessary
				5
		);
	}

	@ParameterizedTest(name = "{0}, {1}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-3349")
	void override_invalid_0(SingleTypeLoadingModel<T> model, SingleTypeLoadingMapping mapping) {
		init( model, mapping );
		assertThatThrownBy( () -> testLoadingFetchSize(
				// Do not configure search.loading.fetch_size
				null,
				// Override fetch size at query level with this value
				0,
				// Persist that many entities
				100,
				// This does not matter, an exception should be thrown
				0
		) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'fetchSize' must be strictly positive" );
	}

	@ParameterizedTest(name = "{0}, {1}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-3349")
	void override_invalid_negative(SingleTypeLoadingModel<T> model, SingleTypeLoadingMapping mapping) {
		init( model, mapping );
		assertThatThrownBy( () -> testLoadingFetchSize(
				// Do not configure search.loading.fetch_size
				null,
				// Override fetch size at query level with this value
				-1,
				// Persist that many entities
				100,
				// This does not matter, an exception should be thrown
				0
		) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'fetchSize' must be strictly positive" );
	}

	/**
	 * Test a fetch size causing multiple query executions with the last execution involving fewer identifiers
	 * when the collection associations are loaded eagerly.
	 * This used to fail with FetchMode.SUBSELECT.
	 */
	@ParameterizedTest(name = "{0}, {1}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-4150")
	void multipleStatements_lastWithFewerIds_eagerAssociations(SingleTypeLoadingModel<T> model,
			SingleTypeLoadingMapping mapping) {
		init( model, mapping );
		testLoadingFetchSize(
				// Set a fetch size lower than the number of entities
				null, 100,
				// Persist that many entities
				150,
				// 150 entities to load with a fetch size of 100 => 2 fetches are necessary
				2,
				// Make sure collection associations are loaded eagerly,
				// so as to always trigger a subselect for associations with FetchMode.SUBSELECT.
				model.getEagerGraphName()
		);
	}

	private void testLoadingFetchSize(Integer searchLoadingFetchSize, Integer overriddenFetchSize,
			int entityCount, int expectStatementExecutionCount) {
		testLoadingFetchSize( searchLoadingFetchSize, overriddenFetchSize, entityCount, expectStatementExecutionCount,
				null );
	}

	private void testLoadingFetchSize(
			Integer searchLoadingFetchSize,
			Integer overriddenFetchSize,
			int entityCount,
			int expectStatementExecutionCount,
			String entityGraph) {
		setup( searchLoadingFetchSize );

		persistThatManyEntities( entityCount );

		testLoadingThatManyEntities(
				session -> {}, // No particular session setup
				o -> {
					if ( entityGraph != null ) {
						o.graph( entityGraph, GraphSemantic.LOAD );
					}
					if ( overriddenFetchSize != null ) {
						o.fetchSize( overriddenFetchSize );
					}
				},
				entityCount,
				c -> {
					c.assertStatementExecutionCount()
							.isEqualTo( expectStatementExecutionCount );
				}
		);
	}

	public void setup(Integer searchLoadingFetchSize) {
		backendMock.expectAnySchema( model.getIndexName() );

		sessionFactory = ormSetupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.QUERY_LOADING_FETCH_SIZE,
						searchLoadingFetchSize
				)
				.withConfiguration( c -> mapping.configure( c, model ) )
				.setup();

		backendMock.verifyExpectationsMet();
	}
}
