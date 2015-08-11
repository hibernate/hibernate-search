/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin.time.impl;

import java.time.Year;

/**
 * Converts a {@link Year} to an {@link Integer}.
 *
 * @author Davide D'Alto
 */
public class YearNumericBridge extends JavaTimeNumericBridge<Year, Integer> {

	public static final YearNumericBridge INSTANCE = new YearNumericBridge();

	private YearNumericBridge() {
	}

	/**
	 * @see Year#getValue()
	 */
	@Override
	public Integer encode(Year year) {
		return year.getValue();
	}

	/**
	 * @see Year#of(int)
	 */
	@Override
	public Year decode(Integer number) {
		return Year.of( number );
	}
}
