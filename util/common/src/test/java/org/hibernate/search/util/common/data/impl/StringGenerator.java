/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
