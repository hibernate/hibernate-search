package org.hibernate.search.engine;

/**
 * Interface to implement boost values as functions
 * of the object value being boosted.
 * Implementations must be threadsafe.
 *
 * @author Hardy Ferentschik
 * @see org.hibernate.search.annotations.Boost
 */
public interface BoostStrategy {
	public float defineBoost(Object value);
}
