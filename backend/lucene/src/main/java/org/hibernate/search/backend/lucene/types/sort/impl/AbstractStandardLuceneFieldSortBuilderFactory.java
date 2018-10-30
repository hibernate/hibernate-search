/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortBuilder;
import org.hibernate.search.backend.lucene.types.converter.impl.LuceneFieldConverter;
import org.hibernate.search.engine.logging.spi.EventContexts;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.impl.common.LoggerFactory;

abstract class AbstractStandardLuceneFieldSortBuilderFactory<F> implements LuceneFieldSortBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final boolean sortable;

	protected final LuceneFieldConverter<F, ?> converter;

	protected AbstractStandardLuceneFieldSortBuilderFactory(boolean sortable, LuceneFieldConverter<F, ?> converter) {
		this.sortable = sortable;
		this.converter = converter;
	}

	@Override
	public DistanceSortBuilder<LuceneSearchSortBuilder> createDistanceSortBuilder(String absoluteFieldPath,
			GeoPoint center) {
		throw log.distanceOperationsNotSupportedByFieldType(
				EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
		);
	}

	@Override
	public boolean isDslCompatibleWith(LuceneFieldSortBuilderFactory obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj.getClass() != this.getClass() ) {
			return false;
		}

		AbstractStandardLuceneFieldSortBuilderFactory<?> other = (AbstractStandardLuceneFieldSortBuilderFactory<?>) obj;

		return sortable == other.sortable &&
				converter.isDslCompatibleWith( other.converter );
	}

	protected void checkSortable(String absoluteFieldPath) {
		if ( !sortable ) {
			throw log.unsortableField( absoluteFieldPath,
					EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
		}
	}
}
