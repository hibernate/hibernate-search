/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexValueFieldType;
import org.hibernate.search.engine.backend.types.converter.FromDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentValueConverter;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeOptionsStep;
import org.hibernate.search.util.common.impl.Contracts;

abstract class AbstractElasticsearchIndexFieldTypeOptionsStep<S extends AbstractElasticsearchIndexFieldTypeOptionsStep<?, F>, F>
		implements IndexFieldTypeOptionsStep<S, F> {
	protected final ElasticsearchIndexFieldTypeBuildContext buildContext;
	protected final ElasticsearchIndexValueFieldType.Builder<F> builder;

	AbstractElasticsearchIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext,
			Class<F> valueType, PropertyMapping mapping) {
		this.buildContext = buildContext;
		this.builder = new ElasticsearchIndexValueFieldType.Builder<>( valueType, mapping );
	}

	@Override
	public <V> S dslConverter(Class<V> valueType, ToDocumentValueConverter<V, ? extends F> toIndexConverter) {
		Contracts.assertNotNull( valueType, "valueType" );
		Contracts.assertNotNull( toIndexConverter, "toIndexConverter" );
		builder.dslConverter( valueType, toIndexConverter );
		return thisAsS();
	}

	@Override
	public <V> S projectionConverter(Class<V> valueType, FromDocumentValueConverter<? super F, V> fromIndexConverter) {
		Contracts.assertNotNull( valueType, "valueType" );
		Contracts.assertNotNull( fromIndexConverter, "fromIndexConverter" );
		builder.projectionConverter( valueType, fromIndexConverter );
		return thisAsS();
	}

	protected abstract S thisAsS();

}
