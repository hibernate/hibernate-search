/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.lang.invoke.MethodHandles;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.SortField;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.impl.DeferredInitializationIndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.IndexSchemaFieldTypedContext;
import org.hibernate.search.engine.backend.document.model.Sortable;
import org.hibernate.search.engine.backend.document.model.Store;
import org.hibernate.search.backend.lucene.document.model.LuceneIndexSchemaFieldTypedContext;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneFieldSortContributor;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeContributor;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.document.model.impl.SortMissingValue;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.engine.search.dsl.sort.SortOrder;
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

	abstract static class AbstractScalarLuceneFieldSortContributor implements LuceneFieldSortContributor {

		private Object sortMissingValueFirstPlaceholder;

		private Object sortMissingValueLastPlaceholder;

		protected AbstractScalarLuceneFieldSortContributor(Object sortMissingValueFirstPlaceholder, Object sortMissingValueLastPlaceholder) {
			this.sortMissingValueFirstPlaceholder = sortMissingValueFirstPlaceholder;
			this.sortMissingValueLastPlaceholder = sortMissingValueLastPlaceholder;
		}

		protected void setEffectiveMissingValue(SortField sortField, Object missingValue, SortOrder order) {
			if ( missingValue == null ) {
				return;
			}

			// TODO so this is to mimic the Elasticsearch behavior, I'm not totally convinced it's the good choice though
			Object effectiveMissingValue;
			if ( missingValue == SortMissingValue.MISSING_FIRST ) {
				effectiveMissingValue = order == SortOrder.DESC ? sortMissingValueLastPlaceholder : sortMissingValueFirstPlaceholder;
			}
			else if ( missingValue == SortMissingValue.MISSING_LAST ) {
				effectiveMissingValue = order == SortOrder.DESC ? sortMissingValueFirstPlaceholder : sortMissingValueLastPlaceholder;
			}
			else {
				effectiveMissingValue = missingValue;
			}

			sortField.setMissingValue( effectiveMissingValue );
		}
	}
}
