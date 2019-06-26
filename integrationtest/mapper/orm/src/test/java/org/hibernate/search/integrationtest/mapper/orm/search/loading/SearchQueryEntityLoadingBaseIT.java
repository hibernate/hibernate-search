/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading;

import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Basic tests of entity loading when executing a search query
 * when only a single type is involved.
 */
@RunWith(Parameterized.class)
public class SearchQueryEntityLoadingBaseIT<T> extends AbstractSearchQueryEntityLoadingSingleTypeIT<T> {

	@Parameterized.Parameters(name = "{0}")
	public static List<SingleTypeLoadingModelPrimitives<?>> data() {
		return allSingleTypeLoadingModelPrimitives();
	}

	private SessionFactory sessionFactory;

	public SearchQueryEntityLoadingBaseIT(SingleTypeLoadingModelPrimitives<T> primitives) {
		super( primitives );
	}

	@Before
	public void setup() {
		backendMock.expectAnySchema( primitives.getIndexName() );

		sessionFactory = ormSetupHelper.withBackendMock( backendMock )
				.setup( primitives.getIndexedClass() );

		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test loading without any specific configuration.
	 */
	@Test
	public void simple() {
		// We don't care about what is indexed exactly, so use the lenient mode
		backendMock.inLenientMode( () ->
				OrmUtils.withinTransaction( sessionFactory, session -> {
					session.persist( primitives.newIndexed( 1 ) );
					session.persist( primitives.newIndexed( 2 ) );
					session.persist( primitives.newIndexed( 3 ) );
				} )
		);

		testLoading(
				c -> c
						.doc( primitives.getIndexName(), primitives.getDocumentIdForEntityId( 1 ) )
						.doc( primitives.getIndexName(), primitives.getDocumentIdForEntityId( 2 ) )
						.doc( primitives.getIndexName(), primitives.getDocumentIdForEntityId( 3 ) ),
				c -> c
						.entity( primitives.getIndexedClass(), 1 )
						.entity( primitives.getIndexedClass(), 2 )
						.entity( primitives.getIndexedClass(), 3 ),
				// Only one entity type means only one statement should be executed, even if there are multiple hits
				c -> c.assertStatementExecutionCount().isEqualTo( 1 )
		);
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
		// We don't care about what is indexed exactly, so use the lenient mode
		backendMock.inLenientMode( () ->
				OrmUtils.withinTransaction( sessionFactory, session -> {
					session.persist( primitives.newIndexed( 1 ) );
					session.persist( primitives.newIndexed( 3 ) );
				} )
		);

		testLoading(
				c -> c
						.doc( primitives.getIndexName(), primitives.getDocumentIdForEntityId( 1 ) )
						.doc( primitives.getIndexName(), primitives.getDocumentIdForEntityId( 2 ) )
						.doc( primitives.getIndexName(), primitives.getDocumentIdForEntityId( 3 ) ),
				c -> c
						.entity( primitives.getIndexedClass(), 1 )
						.entity( primitives.getIndexedClass(), 3 ),
				// Only one entity type means only one statement should be executed, even if there are multiple hits
				c -> c.assertStatementExecutionCount().isEqualTo( 1 )
		);
	}

	@Override
	protected SessionFactory sessionFactory() {
		return sessionFactory;
	}

}
