/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.search.loading;

import java.util.Collections;
import java.util.function.Consumer;

import org.hibernate.search.integrationtest.mapper.pojo.search.loading.model.singletype.BasicIndexedEntity;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;

import org.junit.jupiter.api.BeforeEach;

import org.assertj.core.api.SoftAssertions;

public abstract class AbstractSearchQueryEntityLoadingSingleTypeIT extends AbstractSearchQueryEntityLoadingIT {

	private SearchMapping mapping;

	@BeforeEach
	void setup() {
		backendMock.expectAnySchema( BasicIndexedEntity.NAME );

		mapping = setupHelper.start()
				.expectCustomBeans()
				.setup( BasicIndexedEntity.class );

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
