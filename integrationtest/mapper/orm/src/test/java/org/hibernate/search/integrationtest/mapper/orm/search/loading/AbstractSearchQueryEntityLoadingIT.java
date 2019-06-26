/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading;

import static org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils.reference;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSoftAssertions;

import org.junit.Rule;

public abstract class AbstractSearchQueryEntityLoadingIT {

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public OrmSetupHelper ormSetupHelper = new OrmSetupHelper();

	protected abstract SessionFactory sessionFactory();

	protected final <T> void testLoading(List<? extends Class<? extends T>> targetClasses,
			List<String> targetIndexes,
			Consumer<DocumentReferenceCollector> hitDocumentReferencesContributor,
			Consumer<EntityCollector<T>> expectedLoadedEntitiesContributor,
			Consumer<OrmSoftAssertions> assertionsContributor) {
		OrmSoftAssertions.withinSession( sessionFactory(), (session, softAssertions) -> {
			SearchSession searchSession = Search.session( session );

			SearchQuery<T> query = searchSession.search( targetClasses )
					.predicate( f -> f.matchAll() )
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

			List<T> loadedEntities = query.fetchHits();

			// Be sure to do this after having executed the query, to avoid polluting the persistence context
			EntityCollector<T> entityCollector = new EntityCollector<>( session );
			expectedLoadedEntitiesContributor.accept( entityCollector );
			List<T> expectedLoadedEntities = entityCollector.collected;

			softAssertions.assertThat( loadedEntities )
					.as(
							"Loaded entities when targeting types " + targetClasses
									+ " and when the backend returns document references " + hitDocumentReferences
					)
					.containsExactlyElementsOf( expectedLoadedEntities );
			assertionsContributor.accept( softAssertions );
		} );
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
