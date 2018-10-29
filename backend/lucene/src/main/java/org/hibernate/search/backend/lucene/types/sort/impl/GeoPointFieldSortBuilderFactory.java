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
import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.engine.logging.spi.EventContexts;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.impl.common.LoggerFactory;

public class GeoPointFieldSortBuilderFactory implements LuceneFieldSortBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Sortable sortable;

	public GeoPointFieldSortBuilderFactory(Sortable sortable) {
		this.sortable = sortable;
	}

	@Override
	public FieldSortBuilder<LuceneSearchSortBuilder> createFieldSortBuilder(String absoluteFieldPath) {
		throw log.traditionalSortNotSupportedByGeoPoint(
				EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
	}

	@Override
	public DistanceSortBuilder<LuceneSearchSortBuilder> createDistanceSortBuilder(String absoluteFieldPath,
			GeoPoint center) {
		checkSortable( absoluteFieldPath );

		return new GeoPointDistanceSortBuilder( absoluteFieldPath, center );
	}

	@Override
	public boolean isDslCompatibleWith(LuceneFieldSortBuilderFactory obj) {
		if ( obj.getClass() != this.getClass() ) {
			return false;
		}

		GeoPointFieldSortBuilderFactory other = (GeoPointFieldSortBuilderFactory) obj;

		return other.sortable == this.sortable;
	}

	protected void checkSortable(String absoluteFieldPath) {
		switch ( sortable ) {
			case YES:
				break;
			case DEFAULT:
			case NO:
				throw log.unsortableField( absoluteFieldPath,
						EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
		}
	}
}
