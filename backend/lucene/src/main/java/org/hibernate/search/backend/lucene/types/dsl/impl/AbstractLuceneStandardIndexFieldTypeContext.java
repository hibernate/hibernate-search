/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.hibernate.search.backend.lucene.types.dsl.LuceneStandardIndexFieldTypeContext;
import org.hibernate.search.backend.lucene.document.model.dsl.impl.LuceneIndexSchemaContext;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeContributor;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.document.spi.IndexSchemaFieldDefinitionHelper;
import org.hibernate.search.util.AssertionFailure;

/**
 * @param <S> The concrete type of this context.
 * @param <F> The type of field values.
 *
 * @author Guillaume Smet
 */
public abstract class AbstractLuceneStandardIndexFieldTypeContext<S extends AbstractLuceneStandardIndexFieldTypeContext<? extends S, F>, F>
		implements LuceneStandardIndexFieldTypeContext<S, F>, LuceneIndexSchemaNodeContributor {

	private final LuceneIndexSchemaContext schemaContext;

	private final IndexSchemaFieldDefinitionHelper<F> helper;

	private final String relativeFieldName;

	protected Projectable projectable = Projectable.DEFAULT;

	protected AbstractLuceneStandardIndexFieldTypeContext(LuceneIndexSchemaContext schemaContext, String relativeFieldName,
			Class<F> fieldType) {
		this.schemaContext = schemaContext;
		this.helper = new IndexSchemaFieldDefinitionHelper<>( schemaContext, fieldType );
		this.relativeFieldName = relativeFieldName;
	}

	@Override
	public S dslConverter(
			ToDocumentFieldValueConverter<?, ? extends F> toIndexConverter) {
		helper.dslConverter( toIndexConverter );
		return thisAsS();
	}

	@Override
	public S projectionConverter(
			FromDocumentFieldValueConverter<? super F, ?> fromIndexConverter) {
		helper.projectionConverter( fromIndexConverter );
		return thisAsS();
	}

	@Override
	public IndexFieldAccessor<F> createAccessor() {
		return helper.createAccessor();
	}

	@Override
	public void contribute(LuceneIndexSchemaNodeCollector collector, LuceneIndexSchemaObjectNode parentNode) {
		contribute( helper, collector, parentNode );
	}

	protected abstract void contribute(IndexSchemaFieldDefinitionHelper<F> helper, LuceneIndexSchemaNodeCollector collector,
			LuceneIndexSchemaObjectNode parentNode);

	@Override
	public S projectable(Projectable projectable) {
		this.projectable = projectable;
		return thisAsS();
	}

	protected abstract S thisAsS();

	protected String getRelativeFieldName() {
		return relativeFieldName;
	}

	protected final LuceneIndexSchemaContext getSchemaContext() {
		return schemaContext;
	}

	protected static boolean resolveDefault(Projectable projectable) {
		switch ( projectable ) {
			case DEFAULT:
			case NO:
				return false;
			case YES:
				return true;
			default:
				throw new AssertionFailure( "Unexpected value for Projectable: " + projectable );
		}
	}

	protected static boolean resolveDefault(Sortable sortable) {
		switch ( sortable ) {
			case DEFAULT:
			case NO:
				return false;
			case YES:
				return true;
			default:
				throw new AssertionFailure( "Unexpected value for Sortable: " + sortable );
		}
	}
}
