/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.predicate.impl;

import static org.hibernate.search.engine.search.query.spi.QueryParametersValueProvider.parameter;
import static org.hibernate.search.engine.search.query.spi.QueryParametersValueProvider.simple;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.common.impl.AbstractElasticsearchCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.AbstractElasticsearchSingleFieldPredicate;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.engine.search.query.spi.QueryParametersValueProvider;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ElasticsearchStandardMatchPredicate extends AbstractElasticsearchSingleFieldPredicate {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final JsonAccessor<JsonElement> QUERY_ACCESSOR = JsonAccessor.root().property( "query" );

	private static final JsonObjectAccessor MATCH_ACCESSOR = JsonAccessor.root().property( "match" ).asObject();

	private final QueryParametersValueProvider<JsonElement> valueProvider;

	ElasticsearchStandardMatchPredicate(Builder<?> builder) {
		super( builder );
		valueProvider = builder.valueProvider;
	}

	@Override
	protected JsonObject doToJsonQuery(PredicateRequestContext context, JsonObject outerObject,
			JsonObject innerObject) {
		QUERY_ACCESSOR.set( innerObject, valueProvider.provide( context.toQueryParametersContext() ) );

		JsonObject middleObject = new JsonObject();
		middleObject.add( absoluteFieldPath, innerObject );

		MATCH_ACCESSOR.set( outerObject, middleObject );
		return outerObject;
	}

	public static class Factory<F>
			extends AbstractElasticsearchCodecAwareSearchQueryElementFactory<MatchPredicateBuilder, F> {
		public Factory(ElasticsearchFieldCodec<F> codec) {
			super( codec );
		}

		@Override
		public MatchPredicateBuilder create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field) {
			return new Builder<>( codec, scope, field );
		}
	}

	static class Builder<F> extends AbstractBuilder implements MatchPredicateBuilder {

		protected final ElasticsearchSearchIndexValueFieldContext<F> field;
		private final ElasticsearchFieldCodec<F> codec;

		private QueryParametersValueProvider<JsonElement> valueProvider;

		Builder(ElasticsearchFieldCodec<F> codec, ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field) {
			super( scope, field );
			this.field = field;
			this.codec = codec;
		}

		@Override
		public void fuzzy(int maxEditDistance, int exactPrefixLength) {
			throw log.fullTextFeaturesNotSupportedByFieldType( EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
		}

		@Override
		public void analyzer(String analyzerName) {
			throw log.fullTextFeaturesNotSupportedByFieldType( EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
		}

		@Override
		public void skipAnalysis() {
			throw log.fullTextFeaturesNotSupportedByFieldType( EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
		}

		@Override
		public void value(Object value, ValueConvert convert) {
			this.valueProvider = simple( convert( scope, codec, field, value, convert ) );
		}

		@Override
		public void param(String parameterName, ValueConvert convert) {
			this.valueProvider = parameter( parameterName, Object.class,
					value -> convert( scope, codec, field, value, convert ) );
		}

		private static <T> JsonElement convert(ElasticsearchSearchIndexScope<?> scope, ElasticsearchFieldCodec<T> codec,
				ElasticsearchSearchIndexValueFieldContext<T> field, Object value, ValueConvert convert) {
			DslConverter<?, ? extends T> dslToIndexConverter = field.type().dslConverter( convert );
			try {
				T converted = dslToIndexConverter.unknownTypeToDocumentValue( value, scope.toDocumentValueConvertContext() );
				return codec.encode( converted );
			}
			catch (RuntimeException e) {
				throw log.cannotConvertDslParameter(
						e.getMessage(), e, EventContexts.fromIndexFieldAbsolutePath( field.absolutePath() )
				);
			}
		}

		@Override
		public SearchPredicate build() {
			return new ElasticsearchStandardMatchPredicate( this );
		}
	}
}
