package org.hibernate.search.query.dsl;

/**
 * @author Emmanuel Bernard
 */
public interface RangeTerminationExcludable extends Termination<RangeTerminationExcludable> {
	RangeTerminationExcludable excludeLimit();
}
