/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.hibernate.search.backend.lucene.types.impl.LuceneIndexFieldType;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeOptionsStep;
import org.hibernate.search.util.common.impl.Contracts;

abstract class AbstractLuceneIndexFieldTypeOptionsStep<S extends AbstractLuceneIndexFieldTypeOptionsStep<?, F>, F>
		implements IndexFieldTypeOptionsStep<S, F> {
	protected final LuceneIndexFieldTypeBuildContext buildContext;
	protected final LuceneIndexFieldType.Builder<F> builder;

	AbstractLuceneIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext, Class<F> valueType) {
		this.buildContext = buildContext;
		this.builder = new LuceneIndexFieldType.Builder<>( valueType );
	}

	@Override
	public <V> S dslConverter(Class<V> valueType, ToDocumentFieldValueConverter<V, ? extends F> toIndexConverter) {
		Contracts.assertNotNull( toIndexConverter, "toIndexConverter" );
		builder.dslConverter( valueType, toIndexConverter );
		return thisAsS();
	}

	@Override
	public <V> S projectionConverter(Class<V> valueType, FromDocumentFieldValueConverter<? super F, V> fromIndexConverter) {
		Contracts.assertNotNull( fromIndexConverter, "fromIndexConverter" );
		builder.projectionConverter( valueType, fromIndexConverter );
		return thisAsS();
	}

	protected abstract S thisAsS();
}
