/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration;

import org.hibernate.search.engine.BoostStrategy;

/**
 * Example for a custom <code>BoostStrategy</code> implementation.
 *
 * @author Hardy Ferentschik
 * @see org.hibernate.search.engine.BoostStrategy
 */
public class CustomFieldBoostStrategy implements BoostStrategy {

	@Override
	public float defineBoost(Object value) {
		String name = (String) value;
		if ( "foobar".equals( name ) ) {
			return 3.0f;
		}
		else {
			return 1.0f;
		}
	}
}
