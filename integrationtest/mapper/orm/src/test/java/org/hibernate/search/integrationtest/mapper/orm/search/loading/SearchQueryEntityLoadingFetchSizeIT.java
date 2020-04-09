/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Test fetch size of entity loading when executing a search query
 * when only a single type is involved.
 */
@RunWith(Parameterized.class)
public class SearchQueryEntityLoadingFetchSizeIT<T> extends AbstractSearchQueryEntityLoadingSingleTypeIT<T> {

	@Parameterized.Parameters(name = "{0}")
	public static List<SingleTypeLoadingModelPrimitives<?>> data() {
		return allSingleTypeLoadingModelPrimitives();
	}

	private SessionFactory sessionFactory;

	public SearchQueryEntityLoadingFetchSizeIT(SingleTypeLoadingModelPrimitives<T> primitives) {
		super( primitives );
	}

	@Before
	public void checkFetchSizeSupported() {
		// TODO HSEARCH-962 fetch size should be supported and testable even when the document id is not the entity id,
		//  once we execute multiple queries in HibernateOrmSingleTypeCriteriaEntityLoader
		Assume.assumeTrue(
				"This test only makes sense if cache lookups are supported",
				primitives.isFetchSizeSupported()
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3349")
	public void defaults() {
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

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3349")
	public void configurationProperty() {
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

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3349")
	public void override_valid() {
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

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3349")
	public void override_invalid_0() {
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

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3349")
	public void override_invalid_negative() {
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

	@Override
	protected SessionFactory sessionFactory() {
		return sessionFactory;
	}

	private void testLoadingFetchSize(
			Integer searchLoadingFetchSize,
			Integer overriddenFetchSize,
			int entityCount,
			int expectStatementExecutionCount) {
		setup( searchLoadingFetchSize );

		persistThatManyEntities( entityCount );

		testLoadingThatManyEntities(
				session -> { }, // No particular session setup
				o -> {
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
		backendMock.expectAnySchema( primitives.getIndexName() );

		sessionFactory = ormSetupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.QUERY_LOADING_FETCH_SIZE,
						searchLoadingFetchSize
				)
				.setup( primitives.getIndexedClass() );

		backendMock.verifyExpectationsMet();
	}
}
