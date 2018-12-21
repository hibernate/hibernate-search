/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldFieldCodec;
import org.hibernate.search.backend.lucene.types.converter.LuceneFieldContributor;
import org.hibernate.search.backend.lucene.types.converter.LuceneFieldValueExtractor;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexFieldType;
import org.hibernate.search.backend.lucene.types.projection.impl.LuceneStandardFieldProjectionBuilderFactory;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTerminalContext;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.PassThroughFromDocumentFieldValueConverter;

/**
 * @author Guillaume Smet
 */
class LuceneFieldIndexFieldTypeContext<F>
		implements IndexSchemaFieldTerminalContext<F> {

	private final FromDocumentFieldValueConverter<? super F, ?> indexToProjectionConverter;
	private final LuceneFieldContributor<F> fieldContributor;
	private final LuceneFieldValueExtractor<F> fieldValueExtractor;

	private final LuceneIndexSchemaFieldDslBackReference<F> fieldDslBackReference;

	LuceneFieldIndexFieldTypeContext(Class<F> fieldType,
			LuceneFieldContributor<F> fieldContributor, LuceneFieldValueExtractor<F> fieldValueExtractor,
			LuceneIndexSchemaFieldDslBackReference<F> fieldDslBackReference) {
		this.indexToProjectionConverter = new PassThroughFromDocumentFieldValueConverter<>( fieldType );
		this.fieldContributor = fieldContributor;
		this.fieldValueExtractor = fieldValueExtractor;
		this.fieldDslBackReference = fieldDslBackReference;
	}

	@Override
	public IndexFieldAccessor<F> createAccessor() {
		return fieldDslBackReference.onCreateAccessor( toIndexFieldType() );
	}

	private LuceneIndexFieldType<F> toIndexFieldType() {
		LuceneFieldFieldCodec<F> codec = new LuceneFieldFieldCodec<>( fieldContributor, fieldValueExtractor );

		return new LuceneIndexFieldType<>(
				codec,
				null,
				null,
				new LuceneStandardFieldProjectionBuilderFactory<>( fieldValueExtractor != null, indexToProjectionConverter, codec )
		);
	}
}
