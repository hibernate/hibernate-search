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
import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;


class LuceneNativeIndexFieldTypeOptionsStep<F>
		extends AbstractLuceneIndexFieldTypeOptionsStep<LuceneNativeIndexFieldTypeOptionsStep<F>, F> {

	private final LuceneFieldContributor<F> fieldContributor;
	private final LuceneFieldValueExtractor<F> fieldValueExtractor;

	LuceneNativeIndexFieldTypeOptionsStep(Class<F> fieldType,
			LuceneFieldContributor<F> fieldContributor, LuceneFieldValueExtractor<F> fieldValueExtractor) {
		super( fieldType );
		this.fieldContributor = fieldContributor;
		this.fieldValueExtractor = fieldValueExtractor;
	}

	@Override
	public IndexFieldType<F> toIndexFieldType() {
		ProjectionConverter<? super F, ?> projectionConverter = createProjectionConverter();
		ProjectionConverter<? super F, F> rawProjectionConverter = createRawProjectionConverter();
		LuceneFieldFieldCodec<F> codec = new LuceneFieldFieldCodec<>( fieldContributor, fieldValueExtractor );

		return new LuceneIndexFieldType<>(
				codec,
				null,
				null,
				new LuceneStandardFieldProjectionBuilderFactory<>(
						fieldValueExtractor != null, projectionConverter, rawProjectionConverter, codec
				),
				null
		);
	}

	@Override
	protected LuceneNativeIndexFieldTypeOptionsStep<F> thisAsS() {
		return this;
	}
}
