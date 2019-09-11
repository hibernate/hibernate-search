/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.sort.impl;

import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.engine.search.sort.spi.ScoreSortBuilder;

class LuceneScoreSortBuilder extends AbstractLuceneSearchSortBuilder
		implements ScoreSortBuilder<LuceneSearchSortBuilder> {

	private static final SortField FIELD_SCORE_ASC = new SortField( null, Type.SCORE, true );

	@Override
	public void buildAndContribute(LuceneSearchSortCollector collector) {
		if ( order == SortOrder.ASC ) {
			collector.collectSortField( FIELD_SCORE_ASC );
		}
		else {
			collector.collectSortField( SortField.FIELD_SCORE );
		}
	}
}
