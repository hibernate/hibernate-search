/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

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

import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermInSetQuery;
import org.apache.lucene.util.BytesRef;

public class LuceneMatchIdPredicate extends AbstractLuceneSearchPredicate {

	private final List<String> values;

	private LuceneMatchIdPredicate(Builder builder) {
		super( builder );
		values = builder.values;
		// Ensure illegal attempts to mutate the predicate will fail
		builder.values = null;
	}

	@Override
	public void checkNestableWithin(String expectedParentNestedPath) {
		// Nothing to do
	}

	@Override
	protected Query doToQuery(PredicateRequestContext context) {
		List<BytesRef> bytesRefs = values.stream().map( BytesRef::new ).collect( Collectors.toList() );
		return new TermInSetQuery( MetadataFields.idFieldName(), bytesRefs );
	}

	static class Builder extends AbstractBuilder implements MatchIdPredicateBuilder {
		private List<String> values = new ArrayList<>();

		Builder(LuceneSearchIndexScope<?> scope) {
			super( scope );
		}

		@Override
		public void value(Object value, ValueConvert valueConvert) {
			DslConverter<?, String> converter = scope.identifier().dslConverter( valueConvert );
			ToDocumentValueConvertContext context = scope.toDocumentValueConvertContext();
			values.add( converter.unknownTypeToDocumentValue( value, context ) );
		}

		@Override
		public SearchPredicate build() {
			return new LuceneMatchIdPredicate( this );
		}
	}
}
