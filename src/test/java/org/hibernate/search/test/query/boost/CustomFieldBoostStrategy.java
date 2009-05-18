//$Id$
package org.hibernate.search.test.query.boost;

import org.hibernate.search.engine.BoostStrategy;

/**
 * Example for a custom <code>BoostStrategy</code> implementation.
 *
 * @author Hardy Ferentschik
 * @see org.hibernate.search.engine.BoostStrategy
 */
public class CustomFieldBoostStrategy implements BoostStrategy {

	public float defineBoost(Object value) {
		String name = ( String ) value;
		if ( "foobar".equals( name ) ) {
			return 3.0f;
		}
		else {
			return 1.0f;
		}
	}
}