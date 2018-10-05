/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.hibernate.search.backend.lucene.document.model.dsl.LuceneStandardIndexSchemaFieldTypedContext;
import org.hibernate.search.backend.lucene.document.model.dsl.impl.LuceneIndexSchemaContext;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeContributor;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.converter.FromIndexFieldValueConverter;
import org.hibernate.search.engine.backend.document.converter.ToIndexFieldValueConverter;
import org.hibernate.search.engine.backend.document.model.dsl.Store;
import org.hibernate.search.engine.backend.document.spi.IndexSchemaFieldDefinitionHelper;

/**
 * @param <S> The concrete type of this context.
 * @param <F> The type of field values.
 *
 * @author Guillaume Smet
 */
public abstract class AbstractLuceneStandardIndexSchemaFieldTypedContext<S extends AbstractLuceneStandardIndexSchemaFieldTypedContext<? extends S, F>, F>
		implements LuceneStandardIndexSchemaFieldTypedContext<S, F>, LuceneIndexSchemaNodeContributor {

	private final LuceneIndexSchemaContext schemaContext;

	private final IndexSchemaFieldDefinitionHelper<F> helper;

	private final String relativeFieldName;

	private Store store = Store.DEFAULT;

	protected AbstractLuceneStandardIndexSchemaFieldTypedContext(LuceneIndexSchemaContext schemaContext, String relativeFieldName,
			Class<F> fieldType) {
		this.schemaContext = schemaContext;
		this.helper = new IndexSchemaFieldDefinitionHelper<>( schemaContext, fieldType );
		this.relativeFieldName = relativeFieldName;
	}

	@Override
	public S dslConverter(
			ToIndexFieldValueConverter<?, ? extends F> toIndexConverter) {
		helper.dslConverter( toIndexConverter );
		return thisAsS();
	}

	@Override
	public S projectionConverter(
			FromIndexFieldValueConverter<? super F, ?> fromIndexConverter) {
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
	public S store(Store store) {
		this.store = store;
		return thisAsS();
	}

	protected abstract S thisAsS();

	protected String getRelativeFieldName() {
		return relativeFieldName;
	}

	protected Store getStore() {
		return store;
	}

	protected final LuceneIndexSchemaContext getSchemaContext() {
		return schemaContext;
	}
}
