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
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaContext;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTypedContext;
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

	private final IndexSchemaFieldDefinitionHelper<F> helper;

	private final String relativeFieldName;

	private Store store;

	protected AbstractLuceneIndexSchemaFieldTypedContext(IndexSchemaContext schemaContext, String relativeFieldName) {
		this.helper = new IndexSchemaFieldDefinitionHelper<>( schemaContext );
		this.relativeFieldName = relativeFieldName;
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
	public IndexSchemaFieldTypedContext<F> store(Store store) {
		this.store = store;
		return this;
	}

	@Override
	public IndexSchemaFieldTypedContext<F> analyzer(String analyzerName) {
		throw log.cannotUseAnalyzerOnFieldType( relativeFieldName, getSchemaContext().getEventContext() );
	}

	@Override
	public IndexSchemaFieldTypedContext<F> normalizer(String normalizerName) {
		throw log.cannotUseNormalizerOnFieldType( relativeFieldName, getSchemaContext().getEventContext() );
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

	protected final IndexSchemaContext getSchemaContext() {
		return helper.getSchemaContext();
	}
}
