/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.document.model.dsl.impl.LuceneIndexSchemaContext;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.converter.FromIndexFieldValueConverter;
import org.hibernate.search.engine.backend.document.converter.ToIndexFieldValueConverter;
import org.hibernate.search.engine.backend.document.model.dsl.StandardIndexSchemaFieldTypedContext;
import org.hibernate.search.engine.backend.document.model.dsl.Store;
import org.hibernate.search.engine.backend.document.spi.IndexSchemaFieldDefinitionHelper;
import org.hibernate.search.backend.lucene.document.model.dsl.LuceneIndexSchemaFieldTypedContext;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeContributor;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * @author Guillaume Smet
 */
public abstract class AbstractLuceneIndexSchemaFieldTypedContext<F>
		implements LuceneIndexSchemaFieldTypedContext<F>, LuceneIndexSchemaNodeContributor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneIndexSchemaContext schemaContext;

	private final IndexSchemaFieldDefinitionHelper<F> helper;

	private final String relativeFieldName;

	private Store store = Store.DEFAULT;

	protected AbstractLuceneIndexSchemaFieldTypedContext(LuceneIndexSchemaContext schemaContext, String relativeFieldName,
			Class<F> fieldType) {
		this.schemaContext = schemaContext;
		this.helper = new IndexSchemaFieldDefinitionHelper<>( schemaContext, fieldType );
		this.relativeFieldName = relativeFieldName;
	}

	@Override
	public StandardIndexSchemaFieldTypedContext<F> dslConverter(
			ToIndexFieldValueConverter<?, ? extends F> toIndexConverter) {
		helper.dslConverter( toIndexConverter );
		return this;
	}

	@Override
	public StandardIndexSchemaFieldTypedContext<F> projectionConverter(
			FromIndexFieldValueConverter<? super F, ?> fromIndexConverter) {
		helper.projectionConverter( fromIndexConverter );
		return this;
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
	public StandardIndexSchemaFieldTypedContext<F> store(Store store) {
		this.store = store;
		return this;
	}

	@Override
	public StandardIndexSchemaFieldTypedContext<F> analyzer(String analyzerName) {
		throw log.cannotUseAnalyzerOnFieldType( relativeFieldName, getSchemaContext().getEventContext() );
	}

	@Override
	public StandardIndexSchemaFieldTypedContext<F> normalizer(String normalizerName) {
		throw log.cannotUseNormalizerOnFieldType( relativeFieldName, getSchemaContext().getEventContext() );
	}

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
