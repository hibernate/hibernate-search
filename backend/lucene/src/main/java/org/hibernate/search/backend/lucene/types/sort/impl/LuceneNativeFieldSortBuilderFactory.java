/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.scope.model.impl.LuceneCompatibilityChecker;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortBuilder;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class LuceneNativeFieldSortBuilderFactory implements LuceneFieldSortBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static final LuceneNativeFieldSortBuilderFactory INSTANCE = new LuceneNativeFieldSortBuilderFactory();

	private LuceneNativeFieldSortBuilderFactory() {
		// Nothing to do
	}

	@Override
	public boolean isSortable() {
		return false;
	}

	@Override
	public boolean hasCompatibleCodec(LuceneFieldSortBuilderFactory other) {
		return other == INSTANCE;
	}

	@Override
	public boolean hasCompatibleConverter(LuceneFieldSortBuilderFactory other) {
		return other == INSTANCE;
	}

	@Override
	public FieldSortBuilder<LuceneSearchSortBuilder> createFieldSortBuilder(LuceneSearchContext searchContext,
			String absoluteFieldPath, String nestedDocumentPath, LuceneCompatibilityChecker converterChecker) {
		throw unsupported( absoluteFieldPath );
	}

	@Override
	public DistanceSortBuilder<LuceneSearchSortBuilder> createDistanceSortBuilder(String absoluteFieldPath,
			String nestedDocumentPath, GeoPoint center) {
		throw unsupported( absoluteFieldPath );
	}

	private SearchException unsupported(String absoluteFieldPath) {
		return log.unsupportedDSLSortsForNativeField(
				EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
		);
	}
}
