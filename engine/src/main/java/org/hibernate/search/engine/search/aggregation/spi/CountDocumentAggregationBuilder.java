package org.hibernate.search.engine.search.aggregation.spi;

public interface CountDocumentAggregationBuilder extends SearchAggregationBuilder<Long> {
	interface TypeSelector {
		CountDocumentAggregationBuilder builder();
	}
}
