/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils.reference;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.search.loading.dsl.SearchLoadingOptionsStep;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSoftAssertions;

import org.junit.Rule;

public abstract class AbstractSearchQueryEntityLoadingIT {

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	protected abstract SessionFactory sessionFactory();

	protected final <T> void testLoading(
			Consumer<Session> sessionSetup,
			List<? extends Class<? extends T>> targetClasses,
			List<String> targetIndexes,
			Consumer<SearchLoadingOptionsStep> loadingOptionsContributor,
			Consumer<DocumentReferenceCollector> hitDocumentReferencesContributor,
			Consumer<EntityCollector<T>> expectedLoadedEntitiesContributor,
			Consumer<OrmSoftAssertions> assertionsContributor) {
		testLoading(
				sessionSetup, targetClasses, targetIndexes,
				loadingOptionsContributor, hitDocumentReferencesContributor, expectedLoadedEntitiesContributor,
				(assertions, ignored) -> assertionsContributor.accept( assertions )
		);
	}

	protected final <T> void testLoading(
			Consumer<Session> sessionSetup,
			List<? extends Class<? extends T>> targetClasses,
			List<String> targetIndexes,
			Consumer<SearchLoadingOptionsStep> loadingOptionsContributor,
			Consumer<DocumentReferenceCollector> hitDocumentReferencesContributor,
			Consumer<EntityCollector<T>> expectedLoadedEntitiesContributor,
			BiConsumer<OrmSoftAssertions, List<T>> assertionsContributor) {
		OrmSoftAssertions.withinSession( sessionFactory(), (session, softAssertions) -> {
			sessionSetup.accept( session );

			softAssertions.resetListenerData();

			SearchSession searchSession = Search.session( session );

			SearchQuery<T> query = searchSession.search( targetClasses )
					.where( f -> f.matchAll() )
					.loading( loadingOptionsContributor )
					.toQuery();

			DocumentReferenceCollector documentReferenceCollector = new DocumentReferenceCollector();
			hitDocumentReferencesContributor.accept( documentReferenceCollector );
			List<DocumentReference> hitDocumentReferences = documentReferenceCollector.collected;

			backendMock.expectSearchObjects(
					targetIndexes,
					b -> { },
					StubSearchWorkBehavior.of(
							hitDocumentReferences.size(),
							hitDocumentReferences
					)
			);

			List<T> loadedEntities = query.fetchAllHits();

			softAssertions.assertThat( loadedEntities )
					.as(
							"Loaded entities when targeting types " + targetClasses
									+ " and when the backend returns document references " + hitDocumentReferences
					)
					.allSatisfy( loadedEntity -> {
						// Loading should fully initialize entities
						assertThat( Hibernate.isInitialized( loadedEntity ) ).isTrue();
					} );

			assertionsContributor.accept( softAssertions, loadedEntities );

			// Be sure to do this after having executed the query and checked loading,
			// because it may trigger additional loading.
			EntityCollector<T> entityCollector = new EntityCollector<>( session );
			expectedLoadedEntitiesContributor.accept( entityCollector );
			List<T> expectedLoadedEntities = entityCollector.collected;

			// Both the expected and actual list may contain proxies: unproxy everything so that equals() works correctly
			List<T> unproxyfiedExpectedLoadedEntities = unproxyAll( expectedLoadedEntities );
			List<T> unproxyfiedLoadedEntities = unproxyAll( loadedEntities );

			softAssertions.assertThat( unproxyfiedLoadedEntities )
					.as(
							"Loaded, then unproxified entities when targeting types " + targetClasses
									+ " and when the backend returns document references " + hitDocumentReferences
					)
					.allSatisfy(
							element -> assertThat( element )
									.isInstanceOfAny( targetClasses.toArray( new Class<?>[0] ) )
					)
					.containsExactlyElementsOf( unproxyfiedExpectedLoadedEntities );
		} );
	}

	// This cast is fine as long as T is not a proxy interface
	@SuppressWarnings("unchecked")
	private <T> List<T> unproxyAll(List<T> entityList) {
		return entityList.stream()
				.map( entity -> (T) Hibernate.unproxy( entity ) )
				.collect( Collectors.toList() );
	}

	protected static class DocumentReferenceCollector {
		private final List<DocumentReference> collected = new ArrayList<>();

		public DocumentReferenceCollector doc(String indexName, String documentId) {
			collected.add( reference( indexName, documentId ) );
			return this;
		}
	}

	protected static class EntityCollector<T> {
		private final Session session;
		private final List<T> collected = new ArrayList<>();

		private EntityCollector(Session session) {
			this.session = session;
		}

		public EntityCollector<T> entity(Class<? extends T> entityType, Object entityId) {
			collected.add( session.getReference( entityType, entityId ) );
			return this;
		}
	}

}
