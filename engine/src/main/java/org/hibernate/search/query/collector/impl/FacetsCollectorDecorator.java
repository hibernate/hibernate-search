/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.collector.impl;

import java.io.IOException;

import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;

/**
 * A custom {@code Collector} used for handling facet requests.
 *
 * @author Hardy Ferentschik
 */
public class FacetsCollectorDecorator extends Collector {
	/**
	 * The next collector in the delegation chain
	 */
	private final Collector nextInChainCollector;

	private final FacetsCollector facetsCollector;

	public FacetsCollectorDecorator(FacetsCollector facetsCollector, Collector nextInChainCollector) {
		this.nextInChainCollector = nextInChainCollector;
		this.facetsCollector = facetsCollector;
	}

	@Override
	public void setNextReader(AtomicReaderContext context) throws IOException {
		facetsCollector.setNextReader( context );
		nextInChainCollector.setNextReader( context );
	}

	@Override
	public void collect(int doc) throws IOException {
		facetsCollector.collect( doc );
		nextInChainCollector.collect( doc );
	}

	@Override
	public void setScorer(Scorer scorer) throws IOException {
		facetsCollector.setScorer( scorer );
		nextInChainCollector.setScorer( scorer );
	}

	@Override
	public boolean acceptsDocsOutOfOrder() {
		return nextInChainCollector.acceptsDocsOutOfOrder();
	}
}
