/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.ValueConvert;

abstract class AbstractLuceneStandardFieldAggregationBuilderFactory<F>
		extends AbstractLuceneFieldAggregationBuilderFactory<F> {

	private final DslConverter<? super F, ? extends F> rawToFieldValueConverter;
	private final ProjectionConverter<? super F, F> rawFromFieldValueConverter;

	AbstractLuceneStandardFieldAggregationBuilderFactory(boolean aggregable,
			DslConverter<?, ? extends F> toFieldValueConverter,
			DslConverter<? super F, ? extends F> rawToFieldValueConverter,
			ProjectionConverter<? super F, ?> fromFieldValueConverter,
			ProjectionConverter<? super F, F> rawFromFieldValueConverter) {
		super( aggregable, toFieldValueConverter, fromFieldValueConverter );
		this.rawToFieldValueConverter = rawToFieldValueConverter;
		this.rawFromFieldValueConverter = rawFromFieldValueConverter;
	}

	protected <T> DslConverter<?, ? extends F> getToFieldValueConverter(
			String absoluteFieldPath, Class<T> expectedType, ValueConvert convert) {
		DslConverter<?, ? extends F> result;
		switch ( convert ) {
			case NO:
				result = rawToFieldValueConverter;
				break;
			case YES:
			default:
				result = toFieldValueConverter;
				break;
		}
		if ( !result.isValidInputType( expectedType ) ) {
			throw log.invalidAggregationInvalidType(
					absoluteFieldPath, expectedType, EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
			);
		}
		return result;
	}

	@SuppressWarnings("unchecked") // We check the cast is legal by asking the converter
	protected <T> ProjectionConverter<? super F, ? extends T> getFromFieldValueConverter(
			String absoluteFieldPath, Class<T> expectedType, ValueConvert convert) {
		ProjectionConverter<? super F, ?> result;
		switch ( convert ) {
			case NO:
				result = rawFromFieldValueConverter;
				break;
			case YES:
			default:
				result = fromFieldValueConverter;
				break;
		}
		if ( !result.isConvertedTypeAssignableTo( expectedType ) ) {
			throw log.invalidAggregationInvalidType(
					absoluteFieldPath, expectedType, EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
			);
		}
		return (ProjectionConverter<? super F, ? extends T>) result;
	}
}
