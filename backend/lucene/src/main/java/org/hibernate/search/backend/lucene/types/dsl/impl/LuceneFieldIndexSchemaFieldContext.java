/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.hibernate.search.backend.lucene.document.impl.LuceneIndexFieldAccessor;
import org.hibernate.search.backend.lucene.document.model.LuceneFieldContributor;
import org.hibernate.search.backend.lucene.document.model.LuceneFieldValueExtractor;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeContributor;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldFieldCodec;
import org.hibernate.search.backend.lucene.types.converter.impl.LuceneStandardFieldConverter;
import org.hibernate.search.backend.lucene.types.projection.impl.LuceneStandardFieldProjectionBuilderFactory;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.document.converter.runtime.ToDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTerminalContext;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaContext;
import org.hibernate.search.engine.backend.document.spi.IndexSchemaFieldDefinitionHelper;
import org.hibernate.search.util.AssertionFailure;

/**
 * @author Guillaume Smet
 */
public class LuceneFieldIndexSchemaFieldContext<F>
		implements IndexSchemaFieldTerminalContext<F>, LuceneIndexSchemaNodeContributor {

	private static final ToDocumentFieldValueConverter<Object, Object> TO_INDEX_FIELD_VALUE_CONVERTER =
			new ToDocumentFieldValueConverter<Object, Object>() {
				@Override
				public Object convert(Object value, ToDocumentFieldValueConvertContext context) {
					return value;
				}

				@Override
				public Object convertUnknown(Object value, ToDocumentFieldValueConvertContext context) {
					throw new AssertionFailure(
							"Attempt to perform an unsafe conversion on a field with native type;"
							+ " this should not have happened since the DSL is disabled for such fields."
							+ " There is a bug in Hibernate Search, please report it."
					);
				}
			};

	@SuppressWarnings("unchecked") // This instance works for any F
	private static <F> ToDocumentFieldValueConverter<F, F> getToIndexFieldValueConverter() {
		return (ToDocumentFieldValueConverter<F, F>) TO_INDEX_FIELD_VALUE_CONVERTER;
	}

	private final IndexSchemaFieldDefinitionHelper<F> helper;
	private final String relativeFieldName;
	private final LuceneFieldContributor<F> fieldContributor;
	private final LuceneFieldValueExtractor<F> fieldValueExtractor;

	public LuceneFieldIndexSchemaFieldContext(IndexSchemaContext schemaContext, String relativeFieldName,
			Class<F> indexFieldType,
			LuceneFieldContributor<F> fieldContributor, LuceneFieldValueExtractor<F> fieldValueExtractor) {
		this.helper = new IndexSchemaFieldDefinitionHelper<>( schemaContext, indexFieldType,
				getToIndexFieldValueConverter() );
		this.relativeFieldName = relativeFieldName;
		this.fieldContributor = fieldContributor;
		this.fieldValueExtractor = fieldValueExtractor;
	}

	@Override
	public IndexFieldAccessor<F> createAccessor() {
		return helper.createAccessor();
	}

	@Override
	public void contribute(LuceneIndexSchemaNodeCollector collector, LuceneIndexSchemaObjectNode parentNode) {
		LuceneStandardFieldConverter<F> converter = new LuceneStandardFieldConverter<>( helper.createUserIndexFieldConverter() );
		LuceneFieldFieldCodec<F> codec = new LuceneFieldFieldCodec<>( fieldContributor, fieldValueExtractor );

		LuceneIndexSchemaFieldNode<F> schemaNode = new LuceneIndexSchemaFieldNode<>(
				parentNode,
				relativeFieldName,
				converter,
				codec,
				null,
				null,
				new LuceneStandardFieldProjectionBuilderFactory<>( fieldValueExtractor != null, codec, converter )
		);

		helper.initialize( new LuceneIndexFieldAccessor<>( schemaNode ) );

		collector.collectFieldNode( schemaNode.getAbsoluteFieldPath(), schemaNode );
	}
}
