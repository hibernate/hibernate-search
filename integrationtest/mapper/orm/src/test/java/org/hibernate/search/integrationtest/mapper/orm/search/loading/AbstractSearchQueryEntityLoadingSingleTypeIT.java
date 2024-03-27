/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.Session;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.singletype.SingleTypeLoadingMapping;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.singletype.SingleTypeLoadingModel;
import org.hibernate.search.mapper.orm.search.loading.dsl.SearchLoadingOptionsStep;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSoftAssertions;

public abstract class AbstractSearchQueryEntityLoadingSingleTypeIT<T> extends AbstractSearchQueryEntityLoadingIT {

	protected static void forAllModelMappingCombinations(
			BiConsumer<SingleTypeLoadingModel<?>, SingleTypeLoadingMapping> consumer) {
		for ( SingleTypeLoadingModel<?> model : SingleTypeLoadingModel.all() ) {
			for ( SingleTypeLoadingMapping mapping : SingleTypeLoadingMapping.all() ) {
				consumer.accept( model, mapping );
			}
		}
	}

	protected abstract SingleTypeLoadingModel<T> model();

	protected abstract SingleTypeLoadingMapping mapping();

	protected final void persistThatManyEntities(int entityCount) {
		// We don't care about what is indexed exactly, so use the lenient mode
		backendMock().inLenientMode( () -> with( sessionFactory() ).runInTransaction( session -> {
			for ( int i = 0; i < entityCount; i++ ) {
				session.persist( model().newIndexed( i, mapping() ) );
			}
		} ) );
	}

	protected final void testLoadingThatManyEntities(
			Consumer<Session> sessionSetup,
			Consumer<SearchLoadingOptionsStep> loadingOptionsContributor,
			int entityCount,
			Consumer<OrmSoftAssertions> assertionsContributor) {
		testLoadingThatManyEntities( sessionSetup, loadingOptionsContributor, entityCount, assertionsContributor,
				null, null );
	}

	protected final void testLoadingThatManyEntities(
			Consumer<Session> sessionSetup,
			Consumer<SearchLoadingOptionsStep> loadingOptionsContributor,
			int entityCount,
			Consumer<OrmSoftAssertions> assertionsContributor,
			Integer timeout, TimeUnit timeUnit) {
		testLoading(
				sessionSetup,
				Collections.singletonList( model().getIndexedClass() ),
				Collections.singletonList( model().getIndexName() ),
				loadingOptionsContributor,
				c -> {
					for ( int i = 0; i < entityCount; i++ ) {
						c.doc( model().getIndexName(), mapping().getDocumentIdForEntityId( i ) );
					}
				},
				c -> {
					for ( int i = 0; i < entityCount; i++ ) {
						c.entity( model().getIndexedClass(), i );
					}
				},
				(assertions, ignored) -> assertionsContributor.accept( assertions ),
				timeout, timeUnit
		);
	}

	protected final void testLoading(
			Consumer<Session> sessionSetup,
			Consumer<SearchLoadingOptionsStep> loadingOptionsContributor,
			Consumer<DocumentReferenceCollector> hitDocumentReferencesContributor,
			Consumer<EntityCollector<T>> expectedLoadedEntitiesContributor,
			Consumer<OrmSoftAssertions> assertionsContributor) {
		testLoading(
				sessionSetup,
				Collections.singletonList( model().getIndexedClass() ),
				Collections.singletonList( model().getIndexName() ),
				loadingOptionsContributor,
				hitDocumentReferencesContributor,
				expectedLoadedEntitiesContributor,
				assertionsContributor
		);
	}

	protected final void testLoading(
			Consumer<Session> sessionSetup,
			Consumer<SearchLoadingOptionsStep> loadingOptionsContributor,
			Consumer<DocumentReferenceCollector> hitDocumentReferencesContributor,
			Consumer<EntityCollector<T>> expectedLoadedEntitiesContributor,
			BiConsumer<OrmSoftAssertions, List<T>> assertionsContributor) {
		testLoading(
				sessionSetup,
				Collections.singletonList( model().getIndexedClass() ),
				Collections.singletonList( model().getIndexName() ),
				loadingOptionsContributor,
				hitDocumentReferencesContributor,
				expectedLoadedEntitiesContributor,
				assertionsContributor,
				null, null
		);
	}

}
