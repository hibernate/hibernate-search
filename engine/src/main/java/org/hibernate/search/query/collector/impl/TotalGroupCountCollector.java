package org.hibernate.search.query.collector.impl;

import java.io.IOException;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.grouping.term.TermAllGroupsCollector;
import org.hibernate.search.query.grouping.GroupingRequest;

public class TotalGroupCountCollector extends TermAllGroupsCollector {
	
	/**
	 * The next collector in the delegation chain
	 */
	private final Collector nextInChainCollector;
	
	public TotalGroupCountCollector(Collector nextInChainCollector, GroupingRequest grouping) {
		super(grouping.getFieldName());
		this.nextInChainCollector = nextInChainCollector;
	}
	
	@Override
	public void setNextReader(AtomicReaderContext context) throws IOException {
		super.setNextReader(context);
		nextInChainCollector.setNextReader( context );
	}

	@Override
	public void collect(int doc) throws IOException {
		super.collect(doc);
		nextInChainCollector.collect( doc );
	}

	@Override
	public void setScorer(Scorer scorer) throws IOException {
		super.setScorer(scorer);
		nextInChainCollector.setScorer( scorer );
	}

	@Override
	public boolean acceptsDocsOutOfOrder() {
		super.acceptsDocsOutOfOrder();
		return nextInChainCollector.acceptsDocsOutOfOrder();
	}

}
