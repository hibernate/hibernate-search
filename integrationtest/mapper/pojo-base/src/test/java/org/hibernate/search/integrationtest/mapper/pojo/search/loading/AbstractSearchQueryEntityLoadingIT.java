/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.search.loading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils.reference;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.PersistenceTypeKey;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.StubLoadingContext;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.extension.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.jupiter.api.extension.RegisterExtension;

import org.assertj.core.api.SoftAssertions;

public abstract class AbstractSearchQueryEntityLoadingIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	protected final StubLoadingContext loadingContext = new StubLoadingContext();

	protected abstract SearchMapping mapping();

	protected final <T> void testLoading(
			List<? extends Class<? extends T>> targetClasses,
			List<String> targetIndexes,
			Consumer<DocumentReferenceCollector> hitDocumentReferencesContributor,
			Consumer<EntityCollector> expectedLoadedEntitiesContributor,
			Consumer<SoftAssertions> assertionsContributor) {
		try ( SearchSession searchSession = mapping().createSessionWithOptions()
				.loading( o -> o.context( StubLoadingContext.class, loadingContext ) )
				.build() ) {
			SearchQueryOptionsStep<?, T, ?, ?, ?> optionsStep = searchSession.search( targetClasses )
					.where( f -> f.matchAll() );

			SearchQuery<T> query = optionsStep
					.toQuery();

			DocumentReferenceCollector documentReferenceCollector = new DocumentReferenceCollector();
			hitDocumentReferencesContributor.accept( documentReferenceCollector );
			List<DocumentReference> hitDocumentReferences = documentReferenceCollector.collected;

			List<T> loadedEntities = getHits( targetIndexes, query, hitDocumentReferences );

			EntityCollector entityCollector = new EntityCollector( loadingContext );
			expectedLoadedEntitiesContributor.accept( entityCollector );
			List<Object> expectedLoadedEntities = entityCollector.collected;

			assertSoftly( softAssertions -> {
				softAssertions.<Object>assertThat( loadedEntities )
						.as(
								"Loaded entities when targeting types " + targetClasses
										+ " and when the backend returns document references " + hitDocumentReferences
						)
						.allSatisfy(
								element -> assertThat( element )
										.isInstanceOfAny( targetClasses.toArray( new Class<?>[0] ) )
						)
						.containsExactlyElementsOf( expectedLoadedEntities );

				assertionsContributor.accept( softAssertions );
			} );
		}
	}

	protected <T> List<T> getHits(List<String> targetIndexes, SearchQuery<T> query,
			List<DocumentReference> hitDocumentReferences) {
		backendMock.expectSearchObjects(
				targetIndexes,
				b -> {},
				StubSearchWorkBehavior.of(
						hitDocumentReferences.size(),
						hitDocumentReferences
				)
		);

		return query.fetchAllHits();
	}

	protected static class DocumentReferenceCollector {
		private final List<DocumentReference> collected = new ArrayList<>();

		public DocumentReferenceCollector doc(String indexName, String documentId) {
			collected.add( reference( indexName, documentId ) );
			return this;
		}
	}

	protected static class EntityCollector {
		private final StubLoadingContext loadingContext;
		private final List<Object> collected = new ArrayList<>();

		private EntityCollector(StubLoadingContext loadingContext) {
			this.loadingContext = loadingContext;
		}

		public <E, I> EntityCollector entity(PersistenceTypeKey<E, I> typeKey, I entityId) {
			collected.add( loadingContext.persistenceMap( typeKey ).get( entityId ) );
			return this;
		}
	}

}
