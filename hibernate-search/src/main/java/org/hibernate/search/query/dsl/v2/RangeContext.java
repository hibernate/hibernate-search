package org.hibernate.search.query.dsl.v2;

/**
 * @author Emmanuel Bernard
 */
public interface RangeContext extends QueryCustomization<RangeContext> {
	/**
	 * field / property the term query is executed on
	 */
	RangeMatchingContext onField(String fieldName);
}
