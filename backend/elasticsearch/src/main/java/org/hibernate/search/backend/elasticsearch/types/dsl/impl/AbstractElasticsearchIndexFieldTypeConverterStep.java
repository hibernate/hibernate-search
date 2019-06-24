/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.PassThroughFromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.PassThroughToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeConverterStep;
import org.hibernate.search.util.common.impl.Contracts;

abstract class AbstractElasticsearchIndexFieldTypeConverterStep<S extends AbstractElasticsearchIndexFieldTypeConverterStep<? extends S, F>, F>
		implements IndexFieldTypeConverterStep<S, F> {
	private final ElasticsearchIndexFieldTypeBuildContext buildContext;
	private final Class<F> fieldType;

	private ToDocumentFieldValueConverter<?, ? extends F> dslToIndexConverter;
	private FromDocumentFieldValueConverter<? super F, ?> indexToProjectionConverter;

	AbstractElasticsearchIndexFieldTypeConverterStep(ElasticsearchIndexFieldTypeBuildContext buildContext,
			Class<F> fieldType) {
		this.buildContext = buildContext;
		this.fieldType = fieldType;
	}

	@Override
	public S dslConverter(ToDocumentFieldValueConverter<?, ? extends F> toIndexConverter) {
		Contracts.assertNotNull( toIndexConverter, "toIndexConverter" );
		this.dslToIndexConverter = toIndexConverter;
		return thisAsS();
	}

	@Override
	public S projectionConverter(FromDocumentFieldValueConverter<? super F, ?> fromIndexConverter) {
		Contracts.assertNotNull( fromIndexConverter, "fromIndexConverter" );
		this.indexToProjectionConverter = fromIndexConverter;
		return thisAsS();
	}

	protected abstract S thisAsS();

	final Class<F> getFieldType() {
		return fieldType;
	}

	final ElasticsearchIndexFieldTypeBuildContext getBuildContext() {
		return buildContext;
	}

	final ToDocumentFieldValueConverter<?, ? extends F> createDslToIndexConverter() {
		return dslToIndexConverter == null ? createToDocumentRawConverter() : dslToIndexConverter;
	}

	final ToDocumentFieldValueConverter<F, ? extends F> createToDocumentRawConverter() {
		return new PassThroughToDocumentFieldValueConverter<>( fieldType );
	}

	final FromDocumentFieldValueConverter<? super F, ?> createIndexToProjectionConverter() {
		return indexToProjectionConverter == null ? createFromDocumentRawConverter() : indexToProjectionConverter;
	}

	final FromDocumentFieldValueConverter<? super F, F> createFromDocumentRawConverter() {
		return new PassThroughFromDocumentFieldValueConverter<>( fieldType );
	}
}
