/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.extraction.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.lucene.search.projection.impl.SearchProjectionExtractContext.DistanceCollectorKey;
import org.hibernate.search.engine.spatial.GeoPoint;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.MultiCollector;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.TotalHitCountCollector;

public class LuceneCollectorsBuilder {

	private final Sort sort;

	private final int maxDocs;

	private final TotalHitCountCollector totalHitCountCollector;

	private boolean requireTopDocs;
	private boolean requireScore;

	private final List<Collector> luceneCollectors = new ArrayList<>();
	private final Map<DistanceCollectorKey, DistanceCollector> distanceCollectors = new HashMap<>();

	public LuceneCollectorsBuilder(Sort sort, int maxDocs) {
		this.sort = sort;
		this.maxDocs = maxDocs;

		this.totalHitCountCollector = new TotalHitCountCollector();
		this.luceneCollectors.add( this.totalHitCountCollector );
	}

	public void requireScore() {
		this.requireTopDocs = true;
		this.requireScore = true;
	}

	public void requireTopDocsCollector() {
		this.requireTopDocs = true;
	}

	public DistanceCollector addDistanceCollector(String absoluteFieldPath, GeoPoint center) {
		this.requireTopDocs = true; // We can't collect distances if we don't know from which documents it should be collected
		DistanceCollector distanceCollector = new DistanceCollector( absoluteFieldPath, center, maxDocs );
		luceneCollectors.add( distanceCollector );
		distanceCollectors.put( new DistanceCollectorKey( absoluteFieldPath, center ), distanceCollector );
		return distanceCollector;
	}

	public LuceneCollectors build() {
		TopDocsCollector<?> topDocsCollector = null;
		Integer scoreSortFieldIndexForRescoring = null;
		boolean requireFieldDocRescoring = false;

		if ( requireTopDocs && maxDocs > 0 ) {
			if ( sort == null ) {
				topDocsCollector = TopScoreDocCollector.create(
						maxDocs,
						// TODO HSEARCH-3517 Avoid tracking the total hit count when possible
						// Note this will also require to change how we combine collectors,
						// as MultiCollector explicitly ignores the total hit count optimization
						Integer.MAX_VALUE
				);
			}
			else {
				if ( requireScore ) {
					// Since https://issues.apache.org/jira/browse/LUCENE-8412 (Lucene 8.0.0),
					// TopFieldCollector returns TopDocs whose ScoreDocs do not contain a score...
					// Thus we will have to set the scores ourselves.
					requireFieldDocRescoring = true;

					// If there's a SCORE sort field, make sure we remember that, so that later we can optimize rescoring
					SortField[] sortFields = sort.getSort();
					for ( int i = 0; i < sortFields.length; i++ ) {
						SortField sortField = sortFields[i];
						if ( sortField.getType() == SortField.Type.SCORE ) {
							scoreSortFieldIndexForRescoring = i;
							break;
						}
					}
				}
				topDocsCollector = TopFieldCollector.create(
						sort,
						maxDocs,
						// TODO HSEARCH-3517 Avoid tracking the total hit count when possible
						// Note this will also require to change how we combine collectors,
						// as MultiCollector explicitly ignores the total hit count optimization
						Integer.MAX_VALUE
				);
			}
			luceneCollectors.add( topDocsCollector );
		}

		Collector compositeCollector;
		if ( luceneCollectors.size() == 1 ) {
			compositeCollector = luceneCollectors.get( 0 );
		}
		else {
			compositeCollector = MultiCollector.wrap( luceneCollectors );
		}

		return new LuceneCollectors(
				topDocsCollector, totalHitCountCollector, compositeCollector, distanceCollectors,
				requireFieldDocRescoring, scoreSortFieldIndexForRescoring
		);
	}
}
