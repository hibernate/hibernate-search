/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import java.lang.invoke.MethodHandles;

import org.apache.lucene.analysis.Analyzer;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.impl.DeferredInitializationIndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.IndexSchemaFieldTypedContext;
import org.hibernate.search.engine.backend.document.model.Sortable;
import org.hibernate.search.engine.backend.document.model.Store;
import org.hibernate.search.backend.lucene.document.model.LuceneIndexSchemaFieldTypedContext;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * @author Guillaume Smet
 */
public abstract class AbstractLuceneIndexSchemaFieldTypedContext<T>
		implements LuceneIndexSchemaFieldTypedContext<T>, LuceneIndexSchemaNodeContributor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private DeferredInitializationIndexFieldAccessor<T> accessor = new DeferredInitializationIndexFieldAccessor<>();

	private final String fieldName;

	private Store store;

	protected AbstractLuceneIndexSchemaFieldTypedContext(String fieldName) {
		this.fieldName = fieldName;
	}

	@Override
	public IndexFieldAccessor<T> createAccessor() {
		return accessor;
	}

	@Override
	public void contribute(LuceneIndexSchemaNodeCollector collector, LuceneIndexSchemaObjectNode parentNode) {
		contribute( accessor, collector, parentNode );
	}

	protected abstract void contribute(DeferredInitializationIndexFieldAccessor<T> reference, LuceneIndexSchemaNodeCollector collector,
			LuceneIndexSchemaObjectNode parentNode);

	@Override
	public IndexSchemaFieldTypedContext<T> store(Store store) {
		this.store = store;
		return this;
	}

	@Override
	public IndexSchemaFieldTypedContext<T> sortable(Sortable sortable) {
		throw log.cannotUseSortableOnFieldType( fieldName );
	}

	@Override
	public IndexSchemaFieldTypedContext<T> analyzer(String analyzerName) {
		throw log.cannotUseAnalyzerOnFieldType( fieldName );
	}

	@Override
	public IndexSchemaFieldTypedContext<T> normalizer(String normalizerName) {
		throw log.cannotUseNormalizerOnFieldType( fieldName );
	}

	protected String getFieldName() {
		return fieldName;
	}

	protected Store getStore() {
		return store;
	}

	protected Analyzer getAnalyzer() {
		return null;
	}

	protected Analyzer getNormalizer() {
		return null;
	}
}
