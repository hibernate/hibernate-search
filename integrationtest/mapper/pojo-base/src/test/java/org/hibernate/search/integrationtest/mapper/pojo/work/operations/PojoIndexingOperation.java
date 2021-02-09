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
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.StubDocumentNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubDocumentWork;

public enum PojoIndexingOperation {

	ADD {
		@Override
		void expect(BackendMock.DocumentWorkCallListContext context, String tenantId,
				String id, String routingKey, String value) {
			context.add( b -> addWorkInfoAndDocument( b, tenantId, id, routingKey, value ) );
		}

		@Override
		void addTo(SearchIndexingPlan indexingPlan, Object providedId, String providedRoutingKey, IndexedEntity entity) {
			indexingPlan.add( providedId, providedRoutingKey, entity );
		}

		@Override
		CompletionStage<?> execute(SearchIndexer indexer, Object providedId, String providedRoutingKey,
				IndexedEntity entity) {
			return indexer.add( providedId, providedRoutingKey, entity );
		}
	},
	ADD_OR_UPDATE {
		@Override
		void expect(BackendMock.DocumentWorkCallListContext context, String tenantId,
				String id, String routingKey, String value) {
			context.addOrUpdate( b -> addWorkInfoAndDocument( b, tenantId, id, routingKey, value ) );
		}

		@Override
		void addTo(SearchIndexingPlan indexingPlan, Object providedId, String providedRoutingKey,
				IndexedEntity entity) {
			indexingPlan.addOrUpdate( providedId, providedRoutingKey, entity );
		}

		@Override
		CompletionStage<?> execute(SearchIndexer indexer, Object providedId, String providedRoutingKey,
				IndexedEntity entity) {
			return indexer.addOrUpdate( providedId, providedRoutingKey, entity );
		}
	},
	DELETE {
		@Override
		void expect(BackendMock.DocumentWorkCallListContext context, String tenantId,
				String id, String routingKey, String value) {
			context.delete( b -> addWorkInfo( b, tenantId, id, routingKey ) );
		}

		@Override
		void addTo(SearchIndexingPlan indexingPlan, Object providedId, String providedRoutingKey,
				IndexedEntity entity) {
			indexingPlan.delete( providedId, providedRoutingKey, entity );
		}

		@Override
		CompletionStage<?> execute(SearchIndexer indexer, Object providedId, String providedRoutingKey,
				IndexedEntity entity) {
			return indexer.delete( providedId, providedRoutingKey, entity );
		}
	},
	PURGE {
		@Override
		void expect(BackendMock.DocumentWorkCallListContext context, String tenantId,
				String id, String routingKey, String value) {
			context.delete( b -> addWorkInfo( b, tenantId, id, routingKey ) );
		}

		@Override
		void addTo(SearchIndexingPlan indexingPlan, Object providedId, String providedRoutingKey,
				IndexedEntity entity) {
			indexingPlan.delete( IndexedEntity.class, providedId, providedRoutingKey );
		}

		@Override
		CompletionStage<?> execute(SearchIndexer indexer, Object providedId, String providedRoutingKey,
				IndexedEntity entity) {
			return indexer.delete( IndexedEntity.class, providedId, providedRoutingKey );
		}
	};

	abstract void expect(BackendMock.DocumentWorkCallListContext context, String tenantId,
			String id, String routingKey, String value);

	final void addTo(SearchIndexingPlan indexingPlan, Object providedId, IndexedEntity entity) {
		addTo( indexingPlan, providedId, null, entity );
	}

	abstract void addTo(SearchIndexingPlan indexingPlan, Object providedId, String providedRoutingKey,
			IndexedEntity entity);

	final CompletionStage<?> execute(SearchIndexer indexer, Object providedId, IndexedEntity entity) {
		return execute( indexer, providedId, null, entity );
	}

	abstract CompletionStage<?> execute(SearchIndexer indexer, Object providedId, String providedRoutingKey,
			IndexedEntity entity);

	static void addWorkInfo(StubDocumentWork.Builder builder, String tenantId,
			String identifier, String routingKey) {
		builder.tenantIdentifier( tenantId );
		builder.identifier( identifier );
		builder.routingKey( routingKey );
	}

	static void addWorkInfoAndDocument(StubDocumentWork.Builder builder, String tenantId,
			String identifier, String routingKey, String value) {
		builder.tenantIdentifier( tenantId );
		builder.identifier( identifier );
		builder.routingKey( routingKey );
		builder.document( StubDocumentNode.document().field( "value", value ).build() );
	}
}
