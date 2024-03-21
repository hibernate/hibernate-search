/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.predicate.impl;

import static org.hibernate.search.engine.search.query.spi.QueryParametersValueProvider.parameterCollectionOrSingle;
import static org.hibernate.search.engine.search.query.spi.QueryParametersValueProvider.simple;

import java.lang.invoke.MethodHandles;
import java.util.Collection;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonArrayAccessor;
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
import org.hibernate.search.engine.search.predicate.spi.TermsPredicateBuilder;
import org.hibernate.search.engine.search.query.spi.QueryParametersValueProvider;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ElasticsearchTermsPredicate extends AbstractElasticsearchSingleFieldPredicate {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final JsonObjectAccessor TERMS_ACCESSOR = JsonAccessor.root().property( "terms" ).asObject();
	private static final JsonObjectAccessor TERM_ACCESSOR = JsonAccessor.root().property( "term" ).asObject();

	// for boolean query:
	private static final JsonObjectAccessor BOOL_ACCESSOR = JsonAccessor.root().property( "bool" ).asObject();
	private static final JsonArrayAccessor MUST_ACCESSOR = JsonAccessor.root().property( "must" ).asArray();

	private static final JsonAccessor<JsonElement> VALUE_ACCESSOR = JsonAccessor.root().property( "value" );

	private final QueryParametersValueProvider<JsonElement[]> termsProvider;
	private boolean allMatch;

	public ElasticsearchTermsPredicate(Builder<?> builder) {
		super( builder );
		this.termsProvider = builder.termsProvider;
		this.allMatch = builder.allMatch;
	}

	@Override
	protected JsonObject doToJsonQuery(PredicateRequestContext context, JsonObject outerObject, JsonObject innerObject) {
		JsonElement[] terms = termsProvider.provide( context.toQueryParametersContext() );
		// single term
		if ( terms.length == 1 ) {
			VALUE_ACCESSOR.set( innerObject, terms[0] );
			JsonObject middleObject = new JsonObject();
			middleObject.add( absoluteFieldPath, innerObject );
			TERM_ACCESSOR.set( outerObject, middleObject );
			return outerObject;
		}

		// multiple terms in OR
		if ( !allMatch ) {
			JsonArray jsonArray = new JsonArray( terms.length );
			for ( JsonElement element : terms ) {
				jsonArray.add( element );
			}

			innerObject.add( absoluteFieldPath, jsonArray );
			TERMS_ACCESSOR.set( outerObject, innerObject );
			return outerObject;
		}

		// multiple terms in AND
		// using a boolean query here:
		JsonArray termsArray = new JsonArray();
		for ( JsonElement element : terms ) {
			JsonObject innerTermObject = new JsonObject();
			innerTermObject.add( absoluteFieldPath, element );
			JsonObject termObject = new JsonObject();
			TERM_ACCESSOR.set( termObject, innerTermObject );
			termsArray.add( termObject );
		}

		MUST_ACCESSOR.set( innerObject, termsArray );
		BOOL_ACCESSOR.set( outerObject, innerObject );
		return outerObject;
	}

	public static class Factory<F>
			extends AbstractElasticsearchCodecAwareSearchQueryElementFactory<TermsPredicateBuilder, F> {

		public Factory(ElasticsearchFieldCodec<F> codec) {
			super( codec );
		}

		@Override
		public TermsPredicateBuilder create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field) {
			return new Builder<>( codec, scope, field );
		}
	}

	private static class Builder<F> extends AbstractBuilder implements TermsPredicateBuilder {

		private final ElasticsearchSearchIndexValueFieldContext<F> field;
		private final ElasticsearchFieldCodec<F> codec;
		private QueryParametersValueProvider<JsonElement[]> termsProvider;
		private boolean allMatch;

		private Builder(ElasticsearchFieldCodec<F> codec, ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field) {
			super( scope, field );
			// Score is always constant for this query
			constantScore();

			this.field = field;
			this.codec = codec;
		}

		@Override
		public void matchingAny(Collection<?> terms, ValueConvert convert) {
			allMatch = false;
			fillTerms( terms, convert );
		}

		@Override
		public void matchingAll(Collection<?> terms, ValueConvert convert) {
			allMatch = true;
			fillTerms( terms, convert );
		}

		@Override
		public void matchingAnyParam(String parameterName, ValueConvert convert) {
			allMatch = false;
			fillTermsParam( parameterName, convert );
		}

		@Override
		public void matchingAllParam(String parameterName, ValueConvert convert) {
			allMatch = true;
			fillTermsParam( parameterName, convert );
		}

		@Override
		public SearchPredicate build() {
			return new ElasticsearchTermsPredicate( this );
		}

		private void fillTerms(Collection<?> terms, ValueConvert convert) {
			DslConverter<?, F> dslConverter = field.type().dslConverter( convert );
			this.termsProvider = simple( encode( scope, codec, terms, dslConverter, absoluteFieldPath ) );
		}

		private void fillTermsParam(String parameterName, ValueConvert convert) {
			DslConverter<?, F> dslConverter = field.type().dslConverter( convert );
			this.termsProvider = parameterCollectionOrSingle( parameterName,
					terms -> encode( scope, codec, terms, dslConverter, absoluteFieldPath ) );
		}

		private static <T> JsonElement[] encode(ElasticsearchSearchIndexScope<?> scope, ElasticsearchFieldCodec<T> codec,
				Collection<?> terms, DslConverter<?, T> dslConverter, String absoluteFieldPath) {
			JsonElement[] result = new JsonElement[terms.size()];
			int i = 0;
			for ( Object term : terms ) {
				result[i++] = encode( scope, codec, term, dslConverter, absoluteFieldPath );
			}

			return result;
		}

		private static <T> JsonElement encode(ElasticsearchSearchIndexScope<?> scope, ElasticsearchFieldCodec<T> codec,
				Object term, DslConverter<?, T> dslConverter, String absoluteFieldPath) {
			try {
				T converted = dslConverter.unknownTypeToDocumentValue( term, scope.toDocumentValueConvertContext() );
				return codec.encode( converted );
			}
			catch (RuntimeException e) {
				throw log.cannotConvertDslParameter(
						e.getMessage(), e, EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
				);
			}
		}
	}
}
