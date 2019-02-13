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
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeConverterContext;
import org.hibernate.search.util.common.impl.Contracts;

abstract class AbstractElasticsearchIndexFieldTypeConverterContext<S extends AbstractElasticsearchIndexFieldTypeConverterContext<? extends S, F>, F>
		implements IndexFieldTypeConverterContext<S, F> {
	private final ElasticsearchIndexFieldTypeBuildContext buildContext;
	private final Class<F> fieldType;

	private ToDocumentFieldValueConverter<?, ? extends F> dslToIndexConverter;
	private FromDocumentFieldValueConverter<? super F, ?> indexToProjectionConverter;

	AbstractElasticsearchIndexFieldTypeConverterContext(ElasticsearchIndexFieldTypeBuildContext buildContext,
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

	final ElasticsearchIndexFieldTypeBuildContext getBuildContext() {
		return buildContext;
	}

	final ToDocumentFieldValueConverter<?, ? extends F> createDslToIndexConverter() {
		return dslToIndexConverter == null ? new PassThroughToDocumentFieldValueConverter<>( fieldType )
				: dslToIndexConverter;
	}

	final FromDocumentFieldValueConverter<? super F, ?> createIndexToProjectionConverter() {
		/*
		 * TODO HSEARCH-3257 when no projection converter is configured, create a projection converter that will throw an exception
		 * with an explicit message.
		 * Currently we create a pass-through converter because users have no way to bypass the converter.
		 */
		return indexToProjectionConverter == null ? new PassThroughFromDocumentFieldValueConverter<>( fieldType )
				: indexToProjectionConverter;
	}
}
