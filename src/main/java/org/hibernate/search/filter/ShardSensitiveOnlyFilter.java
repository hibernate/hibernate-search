package org.hibernate.search.filter;

/**
 * When using this class in @FullTextFilterDef.impl, Hibernate Search
 * considers the filter to be only influencing the sharding strategy.
 *
 * This filter is not applied on the results of the Lucene query.
 *
 * @author Emmanuel Bernard
 */
public interface ShardSensitiveOnlyFilter {
}
