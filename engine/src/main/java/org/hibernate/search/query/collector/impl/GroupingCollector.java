/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.collector.impl;

import java.io.IOException;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.grouping.term.TermAllGroupsCollector;
import org.apache.lucene.search.grouping.term.TermFirstPassGroupingCollector;
import org.hibernate.search.query.grouping.GroupingRequest;

/**
 * A custom {@code Collector} used for handling grouping requests.
 *
 * @author Hardy Ferentschik
 */
public class GroupingCollector extends TermFirstPassGroupingCollector {

	private GroupingRequest grouping;

	/**
	 * The next collector in the delegation chain
	 */
	private final Collector nextInChainCollector;

	/**
	 * We need to use all groups collector only for the total group count to make sure no group is missed.
	 */
	private final TermAllGroupsCollector allGroupsCollector;

	public GroupingCollector(Collector nextInChainCollector, GroupingRequest grouping) throws IOException {
		super( grouping.getFieldName(), grouping.getGroupSort(), grouping.getGroupOffset() + grouping.getTopGroupCount() );
		this.grouping = grouping;

		// total group count calculation can be parameterized.
		if ( grouping.isCalculateTotalGroupCount() ) {
			this.allGroupsCollector = new TotalGroupCountCollector( nextInChainCollector, grouping );
			this.nextInChainCollector = this.allGroupsCollector;
		}
		else {
			this.allGroupsCollector = null;
			this.nextInChainCollector = nextInChainCollector;
		}
	}

	public GroupingRequest getGrouping() {
		return grouping;
	}

	@Override
	public void setNextReader(AtomicReaderContext context) throws IOException {
		super.setNextReader( context );
		nextInChainCollector.setNextReader( context );
	}

	@Override
	public void collect(int doc) throws IOException {
		super.collect( doc );
		nextInChainCollector.collect( doc );
	}

	@Override
	public void setScorer(Scorer scorer) throws IOException {
		super.setScorer( scorer );
		nextInChainCollector.setScorer( scorer );
	}

	@Override
	public boolean acceptsDocsOutOfOrder() {
		super.acceptsDocsOutOfOrder();
		return nextInChainCollector.acceptsDocsOutOfOrder();
	}

	public Integer getTotalGroupCount() {
		if ( allGroupsCollector != null ) {
			return allGroupsCollector.getGroupCount();
		}
		else {
			return null;
		}
	}
}
