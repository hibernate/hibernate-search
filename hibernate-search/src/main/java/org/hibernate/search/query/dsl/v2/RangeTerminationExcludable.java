package org.hibernate.search.query.dsl.v2;

/**
 * @author Emmanuel Bernard
 */
public interface RangeTerminationExcludable extends Termination<RangeTerminationExcludable> {
	RangeTerminationExcludable excludeLimit();
}
