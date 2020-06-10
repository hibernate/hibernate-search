/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.predicate.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.AnalyzerConstants;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchFieldContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.AbstractElasticsearchSingleFieldPredicateBuilder;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateBuilder;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateContext;
import org.hibernate.search.engine.search.predicate.spi.PhrasePredicateBuilder;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

class ElasticsearchTextPhrasePredicateBuilder extends AbstractElasticsearchSingleFieldPredicateBuilder
		implements PhrasePredicateBuilder<ElasticsearchSearchPredicateBuilder> {

	private static final JsonObjectAccessor MATCH_PHRASE_ACCESSOR = JsonAccessor.root().property( "match_phrase" ).asObject();

	private static final JsonAccessor<Integer> SLOP_ACCESSOR = JsonAccessor.root().property( "slop" ).asInteger();
	private static final JsonAccessor<JsonElement> QUERY_ACCESSOR = JsonAccessor.root().property( "query" );
	private static final JsonAccessor<String> ANALYZER_ACCESSOR = JsonAccessor.root().property( "analyzer" ).asString();

	private final ElasticsearchSearchFieldContext<String> field;
	private Integer slop;
	private JsonElement phrase;
	private String analyzer;

	ElasticsearchTextPhrasePredicateBuilder(ElasticsearchSearchFieldContext<String> field) {
		super( field );
		this.field = field;
	}

	@Override
	public void slop(int slop) {
		this.slop = slop;
	}

	@Override
	public void phrase(String phrase) {
		this.phrase = new JsonPrimitive( phrase );
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
	protected JsonObject doBuild(ElasticsearchSearchPredicateContext context,
			JsonObject outerObject, JsonObject innerObject) {
		if ( analyzer == null ) {
			// Check analyzer compatibility for multi-index search
			field.type().searchAnalyzerName();
			field.type().normalizerName();
		}

		QUERY_ACCESSOR.set( innerObject, phrase );
		if ( slop != null ) {
			SLOP_ACCESSOR.set( innerObject, slop );
		}
		if ( analyzer != null ) {
			ANALYZER_ACCESSOR.set( innerObject, analyzer );
		}

		JsonObject middleObject = new JsonObject();
		middleObject.add( absoluteFieldPath, innerObject );

		MATCH_PHRASE_ACCESSOR.set( outerObject, middleObject );
		return outerObject;
	}

}
