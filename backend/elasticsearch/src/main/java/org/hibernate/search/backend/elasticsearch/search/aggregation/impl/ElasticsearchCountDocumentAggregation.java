/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchClientLog;
import org.hibernate.search.backend.elasticsearch.logging.impl.QueryLog;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexCompositeNodeContext;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchSearchQueryExtractContext;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.spi.CountDocumentAggregationBuilder;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementFactory;

import com.google.gson.JsonObject;

public class ElasticsearchCountDocumentAggregation extends AbstractElasticsearchAggregation<Long> {

	private static final JsonAccessor<Long> TOTAL_HITS_VALUE_PROPERTY_ACCESSOR =
			JsonAccessor.root().property( "hits" )
					.property( "total" )
					.property( "value" ).asLong();

	private static final JsonAccessor<Long> RESPONSE_DOC_COUNT_ACCESSOR =
			JsonAccessor.root().property( "doc_count" ).asLong();
	private static final JsonAccessor<Long> RESPONSE_ROOT_DOC_COUNT_ACCESSOR =
			JsonAccessor.root().property( "root_doc_count" ).property( "doc_count" ).asLong();

	public static SearchQueryElementFactory<CountDocumentAggregationBuilder.TypeSelector,
			ElasticsearchSearchIndexScope<?>,
			ElasticsearchSearchIndexCompositeNodeContext> factory(boolean isNested) {
		return new ElasticsearchCountDocumentAggregation.Factory( isNested );
	}

	private final boolean isNested;

	private ElasticsearchCountDocumentAggregation(Builder builder) {
		super( builder );
		this.isNested = builder.isNested;
	}

	@Override
	public Extractor<Long> request(AggregationRequestContext context, AggregationKey<?> key, JsonObject jsonAggregations) {
		return new CountDocumentsExtractor(isNested);
	}

	private record CountDocumentsExtractor(boolean isNested) implements Extractor<Long> {

		@Override
		public Long extract(JsonObject aggregationResult, AggregationExtractContext context) {
			if ( aggregationResult != null ) {
				if ( isNested ) {
					// We must return the number of root documents,
					// not the number of leaf documents that Elasticsearch returns by default.
					return RESPONSE_ROOT_DOC_COUNT_ACCESSOR.get( aggregationResult )
							.orElseThrow( ElasticsearchClientLog.INSTANCE::elasticsearchResponseMissingData );
				}
				else {
					return RESPONSE_DOC_COUNT_ACCESSOR.get( aggregationResult )
							.orElseThrow( ElasticsearchClientLog.INSTANCE::elasticsearchResponseMissingData );
				}
			}
			else if ( context instanceof ElasticsearchSearchQueryExtractContext c ) {
				return TOTAL_HITS_VALUE_PROPERTY_ACCESSOR.get( c.getResponseBody() )
						.orElseThrow( ElasticsearchClientLog.INSTANCE::elasticsearchResponseMissingData );
			}
			throw ElasticsearchClientLog.INSTANCE.elasticsearchResponseMissingData();
		}
	}

	private static class Factory
			implements
			SearchQueryElementFactory<CountDocumentAggregationBuilder.TypeSelector,
					ElasticsearchSearchIndexScope<?>,
					ElasticsearchSearchIndexCompositeNodeContext> {
		private final boolean isNested;

		public Factory(boolean isNested) {
			this.isNested = isNested;
		}

		@Override
		public CountDocumentAggregationBuilder.TypeSelector create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexCompositeNodeContext node) {
			return new ElasticsearchCountDocumentAggregation.TypeSelector( scope, isNested );
		}

		@Override
		public void checkCompatibleWith(SearchQueryElementFactory<?, ?, ?> other) {
			if ( !getClass().equals( other.getClass() ) ) {
				throw QueryLog.INSTANCE.differentImplementationClassForQueryElement( getClass(), other.getClass() );
			}
		}
	}

	private record TypeSelector(ElasticsearchSearchIndexScope<?> scope, boolean isNested)
			implements CountDocumentAggregationBuilder.TypeSelector {

		@Override
		public CountDocumentAggregationBuilder type() {
			return new Builder( scope, isNested );
		}
	}

	private static class Builder extends AbstractBuilder<Long>
			implements CountDocumentAggregationBuilder {
		private final boolean isNested;

		private Builder(ElasticsearchSearchIndexScope<?> scope, boolean isNested) {
			super( scope );
			this.isNested = isNested;
		}

		@Override
		public ElasticsearchCountDocumentAggregation build() {
			return new ElasticsearchCountDocumentAggregation( this );
		}
	}
}
