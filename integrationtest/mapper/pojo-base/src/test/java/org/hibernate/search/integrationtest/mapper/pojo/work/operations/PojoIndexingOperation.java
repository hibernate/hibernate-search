/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.work.operations;

import java.util.concurrent.CompletionStage;

import org.hibernate.search.mapper.javabean.work.SearchIndexer;
import org.hibernate.search.mapper.javabean.work.SearchIndexingPlan;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.StubDocumentNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubDocumentWork;

public enum PojoIndexingOperation {

	ADD {
		@Override
		void expect(BackendMock.DocumentWorkCallListContext context, String tenantId,
				String id, String routingKey, String value, String containedValue) {
			context.add( b -> addWorkInfoAndDocument( b, tenantId, id, routingKey, value, containedValue ) );
		}

		@Override
		<T> void addTo(SearchIndexingPlan indexingPlan, Object providedId, DocumentRoutesDescriptor providedRoutes, T entity) {
			indexingPlan.add( providedId, providedRoutes, entity );
		}

		@Override
		<T> void addWithoutInstanceTo(SearchIndexingPlan indexingPlan, Class<T> entityClass, Object providedId,
				DocumentRoutesDescriptor providedRoutes) {
			indexingPlan.add( entityClass, providedId, providedRoutes );
		}

		@Override
		CompletionStage<?> execute(SearchIndexer indexer, Object providedId, DocumentRoutesDescriptor providedRoutes,
				IndexedEntity entity) {
			return indexer.add( providedId, providedRoutes, entity );
		}

		@Override
		CompletionStage<?> execute(SearchIndexer indexer, Object providedId, DocumentRoutesDescriptor providedRoutes) {
			return indexer.add( IndexedEntity.class, providedId, providedRoutes );
		}
	},
	ADD_OR_UPDATE {
		@Override
		void expect(BackendMock.DocumentWorkCallListContext context, String tenantId,
				String id, String routingKey, String value, String containedValue) {
			context.addOrUpdate( b -> addWorkInfoAndDocument( b, tenantId, id, routingKey, value, containedValue ) );
		}

		@Override
		<T> void addTo(SearchIndexingPlan indexingPlan, Object providedId, DocumentRoutesDescriptor providedRoutes,
				T entity) {
			indexingPlan.addOrUpdate( providedId, providedRoutes, entity );
		}

		@Override
		<T> void addWithoutInstanceTo(SearchIndexingPlan indexingPlan, Class<T> entityClass, Object providedId,
				DocumentRoutesDescriptor providedRoutes) {
			indexingPlan.addOrUpdate( entityClass, providedId, providedRoutes );
		}

		@Override
		CompletionStage<?> execute(SearchIndexer indexer, Object providedId, DocumentRoutesDescriptor providedRoutes,
				IndexedEntity entity) {
			return indexer.addOrUpdate( providedId, providedRoutes, entity );
		}

		@Override
		CompletionStage<?> execute(SearchIndexer indexer, Object providedId, DocumentRoutesDescriptor providedRoutes) {
			return indexer.addOrUpdate( IndexedEntity.class, providedId, providedRoutes );
		}
	},
	DELETE {
		@Override
		void expect(BackendMock.DocumentWorkCallListContext context, String tenantId,
				String id, String routingKey, String value, String containedValue) {
			context.delete( b -> addWorkInfo( b, tenantId, id, routingKey ) );
		}

		@Override
		<T> void addTo(SearchIndexingPlan indexingPlan, Object providedId, DocumentRoutesDescriptor providedRoutes,
				T entity) {
			indexingPlan.delete( providedId, providedRoutes, entity );
		}

		@Override
		<T> void addWithoutInstanceTo(SearchIndexingPlan indexingPlan, Class<T> entityClass, Object providedId,
				DocumentRoutesDescriptor providedRoutes) {
			indexingPlan.delete( entityClass, providedId, providedRoutes );
		}

		@Override
		CompletionStage<?> execute(SearchIndexer indexer, Object providedId, DocumentRoutesDescriptor providedRoutes,
				IndexedEntity entity) {
			return indexer.delete( providedId, providedRoutes, entity );
		}

		@Override
		CompletionStage<?> execute(SearchIndexer indexer, Object providedId, DocumentRoutesDescriptor providedRoutes) {
			return indexer.delete( IndexedEntity.class, providedId, providedRoutes );
		}
	};

	abstract void expect(BackendMock.DocumentWorkCallListContext context, String tenantId,
			String id, String routingKey, String value, String containedValue);

	final <T> void addTo(SearchIndexingPlan indexingPlan, Object providedId, T entity) {
		addTo( indexingPlan, providedId, null, entity );
	}

	abstract <T> void addTo(SearchIndexingPlan indexingPlan, Object providedId, DocumentRoutesDescriptor providedRoutes,
			T entity);

	final <T> void addWithoutInstanceTo(SearchIndexingPlan indexingPlan, Class<T> entityClass, Object providedId) {
		addWithoutInstanceTo( indexingPlan, entityClass, providedId, null );
	}

	abstract <T> void addWithoutInstanceTo(SearchIndexingPlan indexingPlan, Class<T> entityClass, Object providedId,
			DocumentRoutesDescriptor providedRoutes);

	final CompletionStage<?> execute(SearchIndexer indexer, Object providedId, IndexedEntity entity) {
		return execute( indexer, providedId, null, entity );
	}

	abstract CompletionStage<?> execute(SearchIndexer indexer, Object providedId, DocumentRoutesDescriptor providedRoutes,
			IndexedEntity entity);

	final CompletionStage<?> execute(SearchIndexer indexer, Object providedId) {
		return execute( indexer, providedId, (DocumentRoutesDescriptor) null );
	}

	abstract CompletionStage<?> execute(SearchIndexer indexer, Object providedId,
			DocumentRoutesDescriptor providedRoutes);

	static void addWorkInfo(StubDocumentWork.Builder builder, String tenantId,
			String identifier, String routingKey) {
		builder.tenantIdentifier( tenantId );
		builder.identifier( identifier );
		builder.routingKey( routingKey );
	}

	static void addWorkInfoAndDocument(StubDocumentWork.Builder builder, String tenantId,
			String identifier, String routingKey, String value, String containedValue) {
		builder.tenantIdentifier( tenantId );
		builder.identifier( identifier );
		builder.routingKey( routingKey );
		StubDocumentNode.Builder documentBuilder = StubDocumentNode.document();
		documentBuilder.field( "value", value );
		if ( containedValue != null ) {
			documentBuilder.objectField( "contained", b -> b
					.field( "value", containedValue ) );
		}
		builder.document( documentBuilder.build() );
	}
}
