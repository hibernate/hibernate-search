/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortBuilder;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class LuceneNativeFieldSortBuilderFactory<F> implements LuceneFieldSortBuilderFactory<F> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Override
	public boolean isSortable() {
		return false;
	}

	@Override
	public boolean isCompatibleWith(LuceneFieldSortBuilderFactory<?> other) {
		return getClass().equals( other.getClass() );
	}

	@Override
	public FieldSortBuilder<LuceneSearchSortBuilder> createFieldSortBuilder(LuceneSearchContext searchContext,
			LuceneSearchFieldContext<F> field) {
		throw unsupported( field );
	}

	@Override
	public DistanceSortBuilder<LuceneSearchSortBuilder> createDistanceSortBuilder(
			LuceneSearchContext searchContext,
			LuceneSearchFieldContext<F> field,
			GeoPoint center) {
		throw unsupported( field );
	}

	private SearchException unsupported(LuceneSearchFieldContext<?> field) {
		return log.unsupportedDSLSortsForNativeField( field.eventContext() );
	}
}
