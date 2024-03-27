/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.search;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchHitsAssert.assertThatHits;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ElasticsearchShardsFailedExceptionIT {


	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new ).name( "index1" );
	private final SimpleMappedIndex<IndexBinding> index2 = SimpleMappedIndex.of( IndexBinding::new ).name( "index2" );

	public void setup(boolean ignoreShardFailures) {
		setupHelper.start().withIndexes( index, index2 )
				.withBackendProperty( ElasticsearchBackendSettings.QUERY_SHARD_FAILURE_IGNORE, ignoreShardFailures )
				.withIndexProperty(
						index.name(),
						ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_MAPPING_FILE,
						"hsearch-4915/index1.json"
				)
				.withIndexProperty(
						index2.name(),
						ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_MAPPING_FILE,
						"hsearch-4915/index2.json"
				)
				.setup();

		initData();
	}

	@Test
	void failureFetch() {
		setup( false );
		SearchQuery<DocumentReference> query = createQuery();

		assertThatThrownBy( () -> query.fetchAllHits() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Elasticsearch request failed",
						"\"type\": \"query_shard_exception\","
				);
	}

	@Test
	void failureScroll() {
		setup( false );
		SearchQuery<DocumentReference> query = createQuery();

		assertThatThrownBy( () -> query.scroll( 1 ).next() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Elasticsearch request failed",
						"\"type\": \"query_shard_exception\","
				);
	}

	@Test
	void success() {
		setup( true );
		SearchQuery<DocumentReference> query = createQuery();
		assertThatHits( query.fetchAllHits() )
				.hasDocRefHitsExactOrder( index.typeName(), "1" );
	}

	private SearchQuery<DocumentReference> createQuery() {
		StubMappingScope scope = index.createScope( index2 );

		SearchPredicateFactory factory = scope.predicate();
		SearchPredicate predicate = factory.range().field( "field" ).greaterThan( "tex" ).toPredicate();

		return scope.query().where( predicate ).toQuery();
	}

	private void initData() {
		index.bulkIndexer()
				.add( "1", document -> document.addValue( index.binding().normalizedStringField, "text" ) )
				.join();
	}

	private static class IndexBinding {
		final IndexFieldReference<String> normalizedStringField;

		IndexBinding(IndexSchemaElement root) {
			normalizedStringField = root.field( "field", c -> c.asString() ).toReference();
		}
	}
}
