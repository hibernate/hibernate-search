/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.sharding;

import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.documentProvider;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingContext;
import org.hibernate.search.util.impl.integrationtest.common.assertion.NormalizedDocRefHit;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;

/**
 * An abstract base for sharding-related tests.
 */
@PortedFromSearch5(original = "org.hibernate.search.test.shards.ShardsTest")
public abstract class AbstractShardingIT {

	protected final SimpleMappedIndex<IndexBinding> index;

	protected AbstractShardingIT(RoutingMode routingMode) {
		this.index = SimpleMappedIndex.ofAdvanced( ctx -> new IndexBinding( ctx, routingMode ) );
	}

	protected final DocumentReference[] allDocRefs(Map<String, List<String>> docIdByRoutingKey) {
		return docRefs( docIdByRoutingKey.values().stream().flatMap( List::stream ) );
	}

	protected final DocumentReference[] docRefsForRoutingKey(String routingKey, Map<String, List<String>> docIdByRoutingKey) {
		return docRefs( docIdByRoutingKey.get( routingKey ).stream() );
	}

	protected final DocumentReference[] docRefsForRoutingKeys(Collection<String> routingKeys,
			Map<String, List<String>> docIdByRoutingKey) {
		return docRefs( routingKeys.stream().flatMap( routingKey -> docIdByRoutingKey.get( routingKey ).stream() ) );
	}

	protected final DocumentReference[] docRefs(Stream<String> docIds) {
		return NormalizedDocRefHit.of( b -> {
			docIds.forEach( docId -> b.doc( index.typeName(), docId ) );
		} );
	}

	protected final void initData(Map<String, List<String>> docIdByRoutingKey) {
		BulkIndexer indexer = index.bulkIndexer();
		for ( Map.Entry<String, List<String>> entry : docIdByRoutingKey.entrySet() ) {
			String routingKey = entry.getKey();
			for ( String documentId : entry.getValue() ) {
				indexer.add( documentProvider(
						documentId, routingKey,
						document -> document.addValue( index.binding().indexedRoutingKey, routingKey )
				) );
			}
		}
		indexer.join();
	}

	protected static class IndexBinding {
		final IndexFieldReference<String> indexedRoutingKey;

		public IndexBinding(IndexedEntityBindingContext ctx, RoutingMode routingMode) {
			switch ( routingMode ) {
				case EXPLICIT_ROUTING_KEYS:
					ctx.explicitRouting();
					break;
				case DOCUMENT_IDS:
					break;
			}
			this.indexedRoutingKey = ctx.schemaElement().field(
					"indexedRoutingKey", f -> f.asString()
			).toReference();
		}
	}

	protected enum RoutingMode {
		EXPLICIT_ROUTING_KEYS,
		DOCUMENT_IDS
	}
}
