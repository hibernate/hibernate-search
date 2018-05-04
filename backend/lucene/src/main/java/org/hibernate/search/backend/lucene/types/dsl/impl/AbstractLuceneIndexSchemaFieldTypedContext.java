/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.lang.invoke.MethodHandles;

import org.apache.lucene.analysis.Analyzer;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.spi.DeferredInitializationIndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTypedContext;
import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.engine.backend.document.model.dsl.Store;
import org.hibernate.search.backend.lucene.document.model.dsl.LuceneIndexSchemaFieldTypedContext;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeContributor;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * @author Guillaume Smet
 */
public abstract class AbstractLuceneIndexSchemaFieldTypedContext<T>
		implements LuceneIndexSchemaFieldTypedContext<T>, LuceneIndexSchemaNodeContributor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private DeferredInitializationIndexFieldAccessor<T> accessor = new DeferredInitializationIndexFieldAccessor<>();

	private final String relativeFieldName;

	private Store store;

	protected AbstractLuceneIndexSchemaFieldTypedContext(String relativeFieldName) {
		this.relativeFieldName = relativeFieldName;
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
		throw log.cannotUseSortableOnFieldType( relativeFieldName );
	}

	@Override
	public IndexSchemaFieldTypedContext<T> analyzer(String analyzerName) {
		throw log.cannotUseAnalyzerOnFieldType( relativeFieldName );
	}

	@Override
	public IndexSchemaFieldTypedContext<T> normalizer(String normalizerName) {
		throw log.cannotUseNormalizerOnFieldType( relativeFieldName );
	}

	protected String getRelativeFieldName() {
		return relativeFieldName;
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
