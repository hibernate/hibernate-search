/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.predicate.impl;

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

	private final JsonElement term;
	private final JsonElement[] terms;
	private boolean allMatch;

	public ElasticsearchTermsPredicate(Builder<?> builder) {
		super( builder );
		this.term = builder.term;
		this.terms = builder.terms;
		this.allMatch = builder.allMatch;
	}

	@Override
	protected JsonObject doToJsonQuery(PredicateRequestContext context, JsonObject outerObject,
			JsonObject innerObject) {

		// single term
		if ( term != null ) {
			VALUE_ACCESSOR.set( innerObject, term );
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

		private JsonElement term;
		private JsonElement[] terms;
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
		public SearchPredicate build() {
			return new ElasticsearchTermsPredicate( this );
		}

		private void fillTerms(Collection<?> terms, ValueConvert convert) {
			DslConverter<?, F> dslConverter = field.type().dslConverter( convert );

			if ( terms.size() == 1 ) {
				this.term = encode( terms.iterator().next(), dslConverter );
				this.terms = null;
				return;
			}

			this.term = null;
			this.terms = encode( terms, dslConverter );
		}

		private JsonElement[] encode(Collection<?> terms, DslConverter<?, F> dslConverter) {
			JsonElement[] result = new JsonElement[terms.size()];
			int i = 0;
			for ( Object term : terms ) {
				result[i++] = encode( term, dslConverter );
			}

			return result;
		}

		private JsonElement encode(Object term, DslConverter<?, F> dslConverter) {
			try {
				F converted = dslConverter.unknownTypeToDocumentValue( term, scope.toDocumentValueConvertContext() );
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
