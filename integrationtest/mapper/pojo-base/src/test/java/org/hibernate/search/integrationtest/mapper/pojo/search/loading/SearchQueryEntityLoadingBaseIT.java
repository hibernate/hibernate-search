/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.search.loading;

import org.hibernate.search.integrationtest.mapper.pojo.search.loading.model.singletype.BasicIndexedEntity;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Test;

/**
 * Basic tests of entity loading when executing a search query
 * when only a single type is involved.
 */
public class SearchQueryEntityLoadingBaseIT extends AbstractSearchQueryEntityLoadingSingleTypeIT {

	/**
	 * Test loading without any specific configuration.
	 */
	@Test
	public void simple() {
		final int entityCount = 3;

		persistThatManyEntities( entityCount );

		testLoadingThatManyEntities(
				entityCount,
				// Only one entity type means only one loader call, even if there are multiple hits
				c -> c.assertThat( loadingContext.loaderCalls() ).hasSize( 1 )
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
		persistThatManyEntities( 2 );

		testLoading(
				c -> c
						.doc( BasicIndexedEntity.NAME, "0" )
						.doc( BasicIndexedEntity.NAME, "1" )
						.doc( BasicIndexedEntity.NAME, "2" ),
				c -> c
						.entity( BasicIndexedEntity.PERSISTENCE_KEY, 0 )
						.entity( BasicIndexedEntity.PERSISTENCE_KEY, 1 ),
				// Only one entity type means only one loader call, even if there are multiple hits
				c -> c.assertThat( loadingContext.loaderCalls() ).hasSize( 1 )
		);
	}

}
