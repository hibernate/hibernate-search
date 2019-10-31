/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.PassThroughFromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.PassThroughToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeOptionsStep;
import org.hibernate.search.util.common.impl.Contracts;

abstract class AbstractLuceneIndexFieldTypeOptionsStep<S extends AbstractLuceneIndexFieldTypeOptionsStep<?, F>, F>
		implements IndexFieldTypeOptionsStep<S, F> {
	private final Class<F> fieldType;
	private DslConverter<?, ? extends F> dslConverter;
	private ProjectionConverter<? super F, ?> projectionConverter;

	AbstractLuceneIndexFieldTypeOptionsStep(Class<F> fieldType) {
		this.fieldType = fieldType;
	}

	@Override
	public <V> S dslConverter(Class<V> valueType, ToDocumentFieldValueConverter<V, ? extends F> toIndexConverter) {
		Contracts.assertNotNull( toIndexConverter, "toIndexConverter" );
		this.dslConverter = new DslConverter<>( valueType, toIndexConverter );
		return thisAsS();
	}

	@Override
	public <V> S projectionConverter(Class<V> valueType, FromDocumentFieldValueConverter<? super F, V> fromIndexConverter) {
		Contracts.assertNotNull( fromIndexConverter, "fromIndexConverter" );
		this.projectionConverter = new ProjectionConverter<>( valueType, fromIndexConverter );
		return thisAsS();
	}

	protected abstract S thisAsS();

	final DslConverter<?, ? extends F> createDslConverter() {
		return dslConverter == null ? createRawDslConverter() : dslConverter;
	}

	final DslConverter<F, ? extends F> createRawDslConverter() {
		return new DslConverter<>( fieldType, new PassThroughToDocumentFieldValueConverter<>() );
	}

	final ProjectionConverter<? super F, ?> createProjectionConverter() {
		return projectionConverter == null ? createRawProjectionConverter() : projectionConverter;
	}

	final ProjectionConverter<? super F, F> createRawProjectionConverter() {
		return new ProjectionConverter<>( fieldType, new PassThroughFromDocumentFieldValueConverter<>() );
	}
}
