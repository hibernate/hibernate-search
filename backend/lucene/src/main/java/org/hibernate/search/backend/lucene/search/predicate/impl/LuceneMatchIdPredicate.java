/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import static org.hibernate.search.engine.search.query.spi.QueryParametersValueProvider.parameter;
import static org.hibernate.search.engine.search.query.spi.QueryParametersValueProvider.simple;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.backend.lucene.lowlevel.common.impl.MetadataFields;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.MatchIdPredicateBuilder;
import org.hibernate.search.engine.search.query.spi.QueryParametersValueProvider;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermInSetQuery;
import org.apache.lucene.util.BytesRef;

public class LuceneMatchIdPredicate extends AbstractLuceneSearchPredicate {

	private final List<QueryParametersValueProvider<String>> valueProviders;

	private LuceneMatchIdPredicate(Builder builder) {
		super( builder );
		valueProviders = builder.valueProviders;
		// Ensure illegal attempts to mutate the predicate will fail
		builder.valueProviders = null;
	}

	@Override
	public void checkNestableWithin(String expectedParentNestedPath) {
		// Nothing to do
	}

	@Override
	protected Query doToQuery(PredicateRequestContext context) {
		List<BytesRef> bytesRefs = valueProviders.stream()
				.map( provider -> provider.provide( context ) )
				.map( BytesRef::new )
				.collect( Collectors.toList() );
		return new TermInSetQuery( MetadataFields.idFieldName(), bytesRefs );
	}

	static class Builder extends AbstractBuilder implements MatchIdPredicateBuilder {
		private List<QueryParametersValueProvider<String>> valueProviders = new ArrayList<>();

		Builder(LuceneSearchIndexScope<?> scope) {
			super( scope );
		}

		@Override
		public void value(Object value, ValueConvert valueConvert) {
			DslConverter<?, String> converter = scope.identifier().dslConverter( valueConvert );
			ToDocumentValueConvertContext context = scope.toDocumentValueConvertContext();
			valueProviders.add( simple( converter.unknownTypeToDocumentValue( value, context ) ) );
		}

		@Override
		public void param(String parameterName, ValueConvert valueConvert) {
			DslConverter<?, String> converter = scope.identifier().dslConverter( valueConvert );
			ToDocumentValueConvertContext context = scope.toDocumentValueConvertContext();
			valueProviders.add(
					parameter( parameterName, Object.class, value -> converter.unknownTypeToDocumentValue( value, context ) ) );
		}

		@Override
		public SearchPredicate build() {
			return new LuceneMatchIdPredicate( this );
		}
	}
}
