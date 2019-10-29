/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.predicate.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.document.model.esnative.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.scope.model.impl.ElasticsearchCompatibilityChecker;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.util.impl.AnalyzerConstants;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;

class ElasticsearchTextMatchPredicateBuilder extends ElasticsearchStandardMatchPredicateBuilder<String> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final JsonAccessor<Integer> FUZZINESS_ACCESSOR = JsonAccessor.root().property( "fuzziness" ).asInteger();
	private static final JsonAccessor<Integer> PREFIX_LENGTH_ACCESSOR = JsonAccessor.root().property( "prefix_length" ).asInteger();
	private static final JsonAccessor<String> ANALYZER_ACCESSOR = JsonAccessor.root().property( "analyzer" ).asString();

	private final String type;
	private final ElasticsearchCompatibilityChecker analyzerChecker;

	private Integer fuzziness;
	private Integer prefixLength;
	private String analyzer;

	ElasticsearchTextMatchPredicateBuilder(
			ElasticsearchSearchContext searchContext,
			String absoluteFieldPath,
			DslConverter<?, ? extends String> converter, DslConverter<String, ? extends String> rawConverter,
			ElasticsearchCompatibilityChecker converterChecker, ElasticsearchFieldCodec<String> codec,
			String type, ElasticsearchCompatibilityChecker analyzerChecker) {
		super( searchContext, absoluteFieldPath, converter, rawConverter, converterChecker, codec );
		this.type = type;
		this.analyzerChecker = analyzerChecker;
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
		if ( DataTypes.KEYWORD.equals( type ) ) {
			throw log.skipAnalysisOnKeywordField( absoluteFieldPath, EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
		}

		analyzer( AnalyzerConstants.KEYWORD_ANALYZER );
	}

	@Override
	protected JsonObject doBuild(ElasticsearchSearchPredicateContext context, JsonObject outerObject,
			JsonObject innerObject) {
		if ( analyzer == null ) {
			analyzerChecker.failIfNotCompatible();
		}

		if ( fuzziness != null ) {
			FUZZINESS_ACCESSOR.set( innerObject, fuzziness );
		}
		if ( analyzer != null ) {
			ANALYZER_ACCESSOR.set( innerObject, analyzer );
		}
		if ( prefixLength != null ) {
			PREFIX_LENGTH_ACCESSOR.set( innerObject, prefixLength );
		}
		return super.doBuild( context, outerObject, innerObject );
	}
}
