package org.hibernate.search.backend.elasticsearch.types.predicate.impl;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonScalarConversions;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.AnalyzerConstants;
import org.hibernate.search.backend.elasticsearch.search.common.impl.AbstractElasticsearchValueFieldSearchQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.AbstractElasticsearchSingleFieldPredicate;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.PhrasePredicateBuilder;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

public class ElasticsearchTextPhrasePredicate extends AbstractElasticsearchSingleFieldPredicate {

	private static final JsonObjectAccessor MATCH_PHRASE_ACCESSOR =
			JsonAccessor.root().property( "match_phrase" ).unwrapSingletonArrays().asObject();

	private static final JsonAccessor<Integer> SLOP_ACCESSOR =
			JsonAccessor.root().property( "slop" ).unwrapSingletonArrays().asInteger();

	private static final JsonAccessor<JsonNode> QUERY_ACCESSOR = JsonAccessor.root().property( "query" );

	private static final JsonAccessor<String> ANALYZER_ACCESSOR =
			JsonAccessor.root().property( "analyzer" ).unwrapSingletonArrays().asString();

	private final Integer slop;

	private final JsonNode phrase;

	private final String analyzer;

	private ElasticsearchTextPhrasePredicate(Builder builder) {
		super( builder );
		slop = builder.slop;
		phrase = builder.phrase;
		analyzer = builder.analyzer;
	}

	@Override
	protected ObjectNode doToJsonQuery(PredicateRequestContext context, ObjectNode outerObject, ObjectNode innerObject) {
		QUERY_ACCESSOR.set( innerObject, phrase );
		if ( slop != null ) {
			SLOP_ACCESSOR.set( innerObject, slop );
		}
		if ( analyzer != null ) {
			ANALYZER_ACCESSOR.set( innerObject, analyzer );
		}
		ObjectNode middleObject = new ObjectNode( JsonNodeFactory.instance );
		middleObject.set( absoluteFieldPath, innerObject );
		MATCH_PHRASE_ACCESSOR.set( outerObject, middleObject );
		return outerObject;
	}

	public static class Factory
			extends AbstractElasticsearchValueFieldSearchQueryElementFactory<PhrasePredicateBuilder, String> {

		@Override
		public PhrasePredicateBuilder create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<String> field) {
			return new Builder( scope, field );
		}
	}

	private static class Builder extends AbstractBuilder implements PhrasePredicateBuilder {

		private final ElasticsearchSearchIndexValueFieldContext<String> field;

		private Integer slop;

		private JsonNode phrase;

		private String analyzer;

		private Builder(ElasticsearchSearchIndexScope<?> scope, ElasticsearchSearchIndexValueFieldContext<String> field) {
			super( scope, field );
			this.field = field;
		}

		@Override
		public void slop(int slop) {
			this.slop = slop;
		}

		@Override
		public void phrase(String phrase) {
			this.phrase = JsonScalarConversions.toNode( phrase );
		}

		@Override
		public void analyzer(String analyzerName) {
			this.analyzer = analyzerName;
		}

		@Override
		public void skipAnalysis() {
			analyzer( AnalyzerConstants.KEYWORD_ANALYZER );
		}

		@Override
		public SearchPredicate build() {
			if ( analyzer == null ) {
				// Check analyzer compatibility for multi-index search
				field.type().searchAnalyzerName();
				field.type().normalizerName();
			}
			return new ElasticsearchTextPhrasePredicate( this );
		}
	}
}
