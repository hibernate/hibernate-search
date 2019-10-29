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
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeConverterStep;
import org.hibernate.search.util.common.impl.Contracts;

abstract class AbstractElasticsearchIndexFieldTypeConverterStep<S extends AbstractElasticsearchIndexFieldTypeConverterStep<?, F>, F>
		implements IndexFieldTypeConverterStep<S, F> {
	private final ElasticsearchIndexFieldTypeBuildContext buildContext;
	private final Class<F> fieldType;

	private DslConverter<?, ? extends F> dslConverter;
	private ProjectionConverter<? super F, ?> projectionConverter;

	AbstractElasticsearchIndexFieldTypeConverterStep(ElasticsearchIndexFieldTypeBuildContext buildContext,
			Class<F> fieldType) {
		this.buildContext = buildContext;
		this.fieldType = fieldType;
	}

	@Override
	public S dslConverter(ToDocumentFieldValueConverter<?, ? extends F> toIndexConverter) {
		Contracts.assertNotNull( toIndexConverter, "toIndexConverter" );
		this.dslConverter = new DslConverter<>( toIndexConverter );
		return thisAsS();
	}

	@Override
	public S projectionConverter(FromDocumentFieldValueConverter<? super F, ?> fromIndexConverter) {
		Contracts.assertNotNull( fromIndexConverter, "fromIndexConverter" );
		this.projectionConverter = new ProjectionConverter<>( fromIndexConverter );
		return thisAsS();
	}

	protected abstract S thisAsS();

	final Class<F> getFieldType() {
		return fieldType;
	}

	final ElasticsearchIndexFieldTypeBuildContext getBuildContext() {
		return buildContext;
	}

	final DslConverter<?, ? extends F> createDslConverter() {
		return dslConverter == null ? createRawDslConverter() : dslConverter;
	}

	final DslConverter<F, ? extends F> createRawDslConverter() {
		return new DslConverter<>( new PassThroughToDocumentFieldValueConverter<>( fieldType ) );
	}

	final ProjectionConverter<? super F, ?> createProjectionConverter() {
		return projectionConverter == null ? createRawProjectionConverter() : projectionConverter;
	}

	final ProjectionConverter<? super F, F> createRawProjectionConverter() {
		return new ProjectionConverter<>( new PassThroughFromDocumentFieldValueConverter<>( fieldType ) );
	}
}
