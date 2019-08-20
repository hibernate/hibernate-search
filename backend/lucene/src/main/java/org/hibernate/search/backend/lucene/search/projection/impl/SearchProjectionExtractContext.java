/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.extraction.impl.DistanceCollector;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;

public class SearchProjectionExtractContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final IndexSearcher indexSearcher;
	private final Query luceneQuery;
	private final TopDocs topDocs;
	private final Map<DistanceCollectorKey, DistanceCollector> distanceCollectors;

	public SearchProjectionExtractContext(IndexSearcher indexSearcher, Query luceneQuery,
			TopDocs topDocs,
			Map<DistanceCollectorKey, DistanceCollector> distanceCollectors) {
		this.indexSearcher = indexSearcher;
		this.luceneQuery = luceneQuery;
		this.topDocs = topDocs;
		this.distanceCollectors = distanceCollectors;
	}

	public Explanation explain(int docId) {
		try {
			return indexSearcher.explain( luceneQuery, docId );
		}
		catch (IOException e) {
			throw log.ioExceptionOnExplain( e );
		}
	}

	public TopDocs getTopDocs() {
		return topDocs;
	}

	public DistanceCollector getDistanceCollector(String absoluteFieldPath, GeoPoint location) {
		DistanceCollectorKey collectorKey = new DistanceCollectorKey( absoluteFieldPath, location );
		return distanceCollectors.get( collectorKey );
	}

	public Collection<DistanceCollector> getDistanceCollectors() {
		return distanceCollectors.values();
	}

	public Query getLuceneQuery() {
		return luceneQuery;
	}

	public static class DistanceCollectorKey {
		private final String absoluteFieldPath;
		private final GeoPoint location;

		public DistanceCollectorKey(String absoluteFieldPath, GeoPoint location) {
			this.absoluteFieldPath = absoluteFieldPath;
			this.location = location;
		}

		@Override
		public boolean equals(Object obj) {
			if ( obj == this ) {
				return true;
			}
			if ( !( obj instanceof DistanceCollectorKey ) ) {
				return false;
			}

			DistanceCollectorKey other = (DistanceCollectorKey) obj;

			return Objects.equals( this.absoluteFieldPath, other.absoluteFieldPath )
					&& Objects.equals( this.location, other.location );
		}
		@Override
		public int hashCode() {
			return Objects.hash( absoluteFieldPath, location );
		}
	}
}
