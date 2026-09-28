package org.hibernate.search.engine.search.aggregation.spi;

public interface CountValuesAggregationBuilder extends SearchFilterableAggregationBuilder<Long> {
	interface TypeSelector {
		CountValuesAggregationBuilder builder();
	}

	void distinct(boolean distinct);

}
