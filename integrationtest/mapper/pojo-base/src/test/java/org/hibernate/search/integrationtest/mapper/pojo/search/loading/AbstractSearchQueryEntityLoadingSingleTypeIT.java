/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.search.loading;

import java.util.Collections;
import java.util.function.Consumer;

import org.hibernate.search.integrationtest.mapper.pojo.search.loading.model.singletype.BasicIndexedEntity;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.StubSelectionLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;

import org.junit.Before;

import org.assertj.core.api.SoftAssertions;

public abstract class AbstractSearchQueryEntityLoadingSingleTypeIT extends AbstractSearchQueryEntityLoadingIT {

	private SearchMapping mapping;

	@Before
	public void setup() {
		backendMock.expectAnySchema( BasicIndexedEntity.NAME );

		mapping = setupHelper.start()
				.withConfiguration( b -> {
					b.addEntityType( BasicIndexedEntity.class, BasicIndexedEntity.NAME, c ->
							c.selectionLoadingStrategy(
									new StubSelectionLoadingStrategy<>( BasicIndexedEntity.PERSISTENCE_KEY ) ) );
				} )
				.setup();

		backendMock.verifyExpectationsMet();
	}

	@Override
	protected SearchMapping mapping() {
		return mapping;
	}

	protected final void persistThatManyEntities(int entityCount) {
		for ( int i = 0; i < entityCount; i++ ) {
			loadingContext.persistenceMap( BasicIndexedEntity.PERSISTENCE_KEY ).put( i, new BasicIndexedEntity( i ) );
		}
	}

	protected final void testLoadingThatManyEntities(int entityCount,
			Consumer<SoftAssertions> assertionsContributor) {
		testLoading(
				Collections.singletonList( BasicIndexedEntity.class ),
				Collections.singletonList( BasicIndexedEntity.NAME ),
				c -> {
					for ( int i = 0; i < entityCount; i++ ) {
						c.doc( BasicIndexedEntity.NAME, String.valueOf( i ) );
					}
				},
				c -> {
					for ( int i = 0; i < entityCount; i++ ) {
						c.entity( BasicIndexedEntity.PERSISTENCE_KEY, i );
					}
				},
				assertionsContributor
		);
	}

	protected final void testLoading(
			Consumer<DocumentReferenceCollector> hitDocumentReferencesContributor,
			Consumer<EntityCollector> expectedLoadedEntitiesContributor,
			Consumer<SoftAssertions> assertionsContributor) {
		testLoading(
				Collections.singletonList( BasicIndexedEntity.class ),
				Collections.singletonList( BasicIndexedEntity.NAME ),
				hitDocumentReferencesContributor,
				expectedLoadedEntitiesContributor,
				assertionsContributor
		);
	}

}
