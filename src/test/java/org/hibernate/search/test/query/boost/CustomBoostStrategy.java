//$Id$
package org.hibernate.search.test.query.boost;

import org.hibernate.search.engine.BoostStrategy;

/**
 * Example for a custom <code>BoostStrategy</code> implementation.
 *
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 * @see org.hibernate.search.engine.BoostStrategy
 */
public class CustomBoostStrategy implements BoostStrategy {

	public float defineBoost(Object value) {
		DynamicBoostedDescriptionLibrary indexed = ( DynamicBoostedDescriptionLibrary ) value;
		return indexed.getDynScore();
	}
}
