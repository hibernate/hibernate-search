/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.data.impl;

import java.util.stream.IntStream;
import java.util.stream.Stream;

interface StringGenerator {

	Stream<String> stream();


	static StringGenerator integerSequenceAsStrings(int firstElement) {
		return new StringGenerator() {
			@Override
			public String toString() {
				return "Sequence of integers as strings, starting at " + firstElement;
			}

			@Override
			public Stream<String> stream() {
				return IntStream.iterate( firstElement, v -> v + 1 ).mapToObj( Integer::toString );
			}
		};
	}

}
