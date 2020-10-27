/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.spi;

import java.math.BigInteger;

public class NumberScaleConstants {

	public static final BigInteger MIN_LONG_AS_BIGINTEGER = BigInteger.valueOf( Long.MIN_VALUE );
	public static final BigInteger MAX_LONG_AS_BIGINTEGER = BigInteger.valueOf( Long.MAX_VALUE );

	private NumberScaleConstants() {
		// Private constructor, do not use
	}
}
