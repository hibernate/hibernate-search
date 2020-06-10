/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import org.hibernate.search.backend.lucene.scope.model.impl.LuceneCompatibilityChecker;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.lowlevel.common.impl.MetadataFields;
import org.hibernate.search.engine.backend.types.converter.spi.StringToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentIdentifierValueConvertContext;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.spi.MatchIdPredicateBuilder;

public class LuceneMatchIdPredicateBuilder extends AbstractLuceneSearchPredicateBuilder
		implements MatchIdPredicateBuilder<LuceneSearchPredicateBuilder> {

	private static final StringToDocumentIdentifierValueConverter RAW_CONVERTER =
			new StringToDocumentIdentifierValueConverter();

	private final List<String> values = new ArrayList<>();
	private final LuceneSearchContext searchContext;
	private final LuceneCompatibilityChecker converterChecker;
	private final ToDocumentIdentifierValueConverter<?> converter;

	LuceneMatchIdPredicateBuilder(LuceneSearchContext searchContext,
			LuceneCompatibilityChecker converterChecker,
			ToDocumentIdentifierValueConverter<?> converter) {
		this.searchContext = searchContext;
		this.converterChecker = converterChecker;
		this.converter = converter;
	}

	@Override
	public void value(Object value, ValueConvert valueConvert) {
		ToDocumentIdentifierValueConverter<?> dslToDocumentIdConverter =
				getDslToDocumentIdentifierConverter( valueConvert );
		ToDocumentIdentifierValueConvertContext toDocumentIdentifierValueConvertContext =
				searchContext.toDocumentIdentifierValueConvertContext();
		values.add( dslToDocumentIdConverter.convertUnknown( value, toDocumentIdentifierValueConvertContext ) );
	}

	@Override
	public void checkNestableWithin(String expectedParentNestedPath) {
		// Nothing to do
	}

	@Override
	protected Query doBuild(LuceneSearchPredicateContext context) {
		Builder builder = new BooleanQuery.Builder();
		for ( String value : values ) {
			builder.add( termQuery( value ), Occur.SHOULD );
		}
		return builder.build();
	}

	private TermQuery termQuery( String value ) {
		return new TermQuery( new Term( MetadataFields.idFieldName(), value ) );
	}

	private ToDocumentIdentifierValueConverter<?> getDslToDocumentIdentifierConverter(ValueConvert convert) {
		switch ( convert ) {
			case NO:
				return RAW_CONVERTER;
			case YES:
			default:
				converterChecker.failIfNotCompatible();
				return converter;
		}
	}
}
