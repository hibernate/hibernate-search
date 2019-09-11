/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.sharding;

import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexDocumentWorkExecutor;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkExecutor;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingContext;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.util.impl.integrationtest.common.assertion.NormalizedDocRefHit;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;

/**
 * A basic test regarding routing keys when hash-based sharding is enabled.
 */
@PortedFromSearch5(original = "org.hibernate.search.test.shards.ShardsTest")
public abstract class AbstractShardingIT {

	protected static final String INDEX_NAME = "IndexName";

	protected IndexMapping indexMapping;
	protected StubMappingIndexManager indexManager;

	protected static DocumentReference[] allDocRefs(Map<String, List<String>> docIdByRoutingKey) {
		return docRefs( docIdByRoutingKey.values().stream().flatMap( List::stream ) );
	}

	protected static DocumentReference[] docRefsForRoutingKey(String routingKey, Map<String, List<String>> docIdByRoutingKey) {
		return docRefs( docIdByRoutingKey.get( routingKey ).stream() );
	}

	protected static DocumentReference[] docRefs(Stream<String> docIds) {
		return NormalizedDocRefHit.of( b -> {
			docIds.forEach( docId -> b.doc( INDEX_NAME, docId ) );
		} );
	}

	protected final void initData(Map<String, List<String>> docIdByRoutingKey) {
		IndexDocumentWorkExecutor<? extends DocumentElement> documentWorkExecutor =
				indexManager.createDocumentWorkExecutor( DocumentCommitStrategy.NONE );
		List<CompletableFuture<?>> tasks = new ArrayList<>();

		for ( Map.Entry<String, List<String>> entry : docIdByRoutingKey.entrySet() ) {
			String routingKey = entry.getKey();
			for ( String documentId : entry.getValue() ) {
				tasks.add( documentWorkExecutor.add(
						referenceProvider( documentId, routingKey ),
						document -> document.addValue( indexMapping.indexedRoutingKey, routingKey )
				) );
			}
		}
		CompletableFuture.allOf( tasks.toArray( new CompletableFuture<?>[0] ) ).join();

		IndexWorkExecutor indexWorkExecutor = indexManager.createWorkExecutor();
		indexWorkExecutor.flush().join();
	}

	protected class IndexMapping {
		final IndexFieldReference<String> indexedRoutingKey;

		public IndexMapping(IndexedEntityBindingContext ctx, RoutingMode routingMode) {
			switch ( routingMode ) {
				case EXPLICIT_ROUTING_KEYS:
					ctx.explicitRouting();
					break;
				case DOCUMENT_IDS:
					break;
			}
			this.indexedRoutingKey = ctx.getSchemaElement().field(
					"indexedRoutingKey", f -> f.asString()
			).toReference();
		}
	}

	protected enum RoutingMode {
		EXPLICIT_ROUTING_KEYS,
		DOCUMENT_IDS
	}
}
