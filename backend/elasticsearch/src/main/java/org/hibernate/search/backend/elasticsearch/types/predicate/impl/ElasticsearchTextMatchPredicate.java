/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.predicate.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.AnalyzerConstants;
import org.hibernate.search.backend.elasticsearch.search.common.impl.AbstractElasticsearchCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;

public class ElasticsearchTextMatchPredicate extends ElasticsearchStandardMatchPredicate {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final JsonAccessor<Integer> FUZZINESS_ACCESSOR = JsonAccessor.root().property( "fuzziness" ).asInteger();
	private static final JsonAccessor<Integer> PREFIX_LENGTH_ACCESSOR =
			JsonAccessor.root().property( "prefix_length" ).asInteger();
	private static final JsonAccessor<String> ANALYZER_ACCESSOR = JsonAccessor.root().property( "analyzer" ).asString();

	private final Integer fuzziness;
	private final Integer prefixLength;
	private final String analyzer;

	private ElasticsearchTextMatchPredicate(Builder builder) {
		super( builder );
		fuzziness = builder.fuzziness;
		prefixLength = builder.prefixLength;
		analyzer = builder.analyzer;
	}

	@Override
	protected JsonObject doToJsonQuery(PredicateRequestContext context, JsonObject outerObject,
			JsonObject innerObject) {
		if ( fuzziness != null ) {
			FUZZINESS_ACCESSOR.set( innerObject, fuzziness );
		}
		if ( analyzer != null ) {
			ANALYZER_ACCESSOR.set( innerObject, analyzer );
		}
		if ( prefixLength != null ) {
			PREFIX_LENGTH_ACCESSOR.set( innerObject, prefixLength );
		}
		return super.doToJsonQuery( context, outerObject, innerObject );
	}

	public static class Factory
			extends AbstractElasticsearchCodecAwareSearchQueryElementFactory<MatchPredicateBuilder, String> {
		public Factory(ElasticsearchFieldCodec<String> codec) {
			super( codec );
		}

		@Override
		public MatchPredicateBuilder create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<String> field) {
			return new Builder( codec, scope, field );
		}
	}

	private static class Builder extends ElasticsearchStandardMatchPredicate.Builder<String> {
		private Integer fuzziness;
		private Integer prefixLength;
		private String analyzer;

		private Builder(ElasticsearchFieldCodec<String> codec, ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<String> field) {
			super( codec, scope, field );
		}

		@Override
		public void fuzzy(int maxEditDistance, int exactPrefixLength) {
			this.fuzziness = maxEditDistance;
			this.prefixLength = exactPrefixLength;
		}

		@Override
		public void analyzer(String analyzerName) {
			this.analyzer = analyzerName;
		}

		@Override
		public void skipAnalysis() {
			if ( field.type().hasNormalizerOnAtLeastOneIndex() ) {
				throw log.skipAnalysisOnNormalizedField( absoluteFieldPath,
						EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
			}

			analyzer( AnalyzerConstants.KEYWORD_ANALYZER );
		}

		@Override
		public SearchPredicate build() {
			if ( analyzer == null ) {
				// Check analyzer compatibility for multi-index search
				field.type().searchAnalyzerName();
				field.type().normalizerName();
			}

			return new ElasticsearchTextMatchPredicate( this );
		}
	}
}
