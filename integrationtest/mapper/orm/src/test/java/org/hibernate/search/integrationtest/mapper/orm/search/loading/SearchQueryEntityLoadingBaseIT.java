/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.SessionFactory;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.singletype.SingleTypeLoadingMapping;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.singletype.SingleTypeLoadingModel;
import org.hibernate.search.util.common.SearchTimeoutException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.TimeoutLoadingListener;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Basic tests of entity loading when executing a search query
 * when only a single type is involved.
 */
@RunWith(Parameterized.class)
public class SearchQueryEntityLoadingBaseIT<T> extends AbstractSearchQueryEntityLoadingSingleTypeIT<T> {

	@Parameterized.Parameters(name = "{0}, {1}")
	public static List<Object[]> params() {
		List<Object[]> result = new ArrayList<>();
		forAllModelMappingCombinations( (model, mapping) -> {
			result.add( new Object[] { model, mapping } );
		} );
		return result;
	}

	@ClassRule
	public static BackendMock backendMock = new BackendMock();

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder = ReusableOrmSetupHolder.withBackendMock( backendMock );

	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	public SearchQueryEntityLoadingBaseIT(SingleTypeLoadingModel<T> model, SingleTypeLoadingMapping mapping) {
		super( model, mapping );
	}

	@Override
	protected BackendMock backendMock() {
		return backendMock;
	}

	@Override
	protected SessionFactory sessionFactory() {
		return setupHolder.sessionFactory();
	}

	@ReusableOrmSetupHolder.SetupParams
	public List<?> setupParams() {
		return Arrays.asList( mapping, model );
	}

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext) {
		backendMock.expectAnySchema( model.getIndexName() );
		setupContext.withConfiguration( c -> mapping.configure( c, model ) );
	}

	/**
	 * Test loading without any specific configuration.
	 */
	@Test
	public void simple() {
		final int entityCount = 3;

		persistThatManyEntities( entityCount );

		testLoadingThatManyEntities(
				session -> {}, // No particular session setup
				o -> {}, // No particular loading option
				entityCount,
				// Only one entity type means only one statement should be executed, even if there are multiple hits
				c -> c.assertStatementExecutionCount().isEqualTo( 1 )
		);
	}

	@Test
	public void simple_withVeryLargeTimeout() {
		final int entityCount = 3;

		persistThatManyEntities( entityCount );

		testLoadingThatManyEntities(
				session -> {}, // No particular session setup
				o -> {}, // No particular loading option
				entityCount,
				// Only one entity type means only one statement should be executed, even if there are multiple hits
				c -> c.assertStatementExecutionCount().isEqualTo( 1 ),
				1, TimeUnit.DAYS
		);
	}

	@Test
	public void simple_entityLoadingTimeout() {
		final int entityCount = 3;

		persistThatManyEntities( entityCount );

		assertThatThrownBy( () -> testLoadingThatManyEntities(
				session -> TimeoutLoadingListener.registerTimingOutLoadingListener( session ),
				o -> {}, // No particular loading option
				entityCount,
				// Only one entity type means only one statement should be executed, even if there are multiple hits
				c -> c.assertStatementExecutionCount().isEqualTo( 1 ),
				1, TimeUnit.MICROSECONDS
		) )
				.isInstanceOf( SearchTimeoutException.class )
				.hasMessageContaining( "Operation exceeded the timeout of 0s, 0ms and 1000ns" );
	}

	/**
	 * Test loading of entities that are not found in the database.
	 * This can happen when the index is slightly out of sync and still has deleted entities in it.
	 * In that case, we expect the loader to return null,
	 * and the backend to skip the corresponding hits.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3349")
	public void notFound() {
		persistThatManyEntities( 2 );

		testLoading(
				session -> {}, // No particular session setup
				o -> {}, // No particular loading option
				c -> c
						.doc( model.getIndexName(), mapping.getDocumentIdForEntityId( 0 ) )
						.doc( model.getIndexName(), mapping.getDocumentIdForEntityId( 1 ) )
						.doc( model.getIndexName(), mapping.getDocumentIdForEntityId( 2 ) ),
				c -> c
						.entity( model.getIndexedClass(), 0 )
						.entity( model.getIndexedClass(), 1 ),
				// Only one entity type means only one statement should be executed, even if there are multiple hits
				c -> c.assertStatementExecutionCount().isEqualTo( 1 )
		);
	}

	/**
	 * Test that returned results are initialized even if a proxy was present in the persistence context.
	 */
	@Test
	public void initializeProxyFromPersistenceContext() {
		final int entityCount = 10;

		persistThatManyEntities( entityCount );

		AtomicReference<Object> proxyReference = new AtomicReference<>();

		testLoadingThatManyEntities(
				session -> {
					/*
					 * Add an entity to the persistence context,
					 * to check that Search does not just get the entities from the persistence context
					 * without initializing them.
					 * testLoading() will assert that search results are not initialized.
					 * NB: "session.getReference" does not load the entity but really creates a proxy.
					 */
					T proxy = session.getReference( model.getIndexedClass(), 1 );
					/*
					 * We need to keep a reference to the proxy, otherwise it will be garbage collected
					 * and ORM (who only holds a weak reference to it) will forget about it.
					 */
					proxyReference.set( proxy );
				},
				o -> {}, // No particular loading option
				entityCount,
				// Only one entity type means only one statement should be executed, even if there are multiple hits
				c -> c.assertStatementExecutionCount().isEqualTo( 1 )
		);
	}

}
