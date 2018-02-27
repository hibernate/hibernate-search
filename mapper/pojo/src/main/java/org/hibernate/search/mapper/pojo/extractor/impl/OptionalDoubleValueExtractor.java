/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.extractor.impl;

import java.util.OptionalDouble;
import java.util.stream.Stream;

public class OptionalDoubleValueExtractor implements ContainerValueExtractor<OptionalDouble, Double> {
	@Override
	public Stream<Double> extract(OptionalDouble container) {
		if ( container != null && container.isPresent() ) {
			return Stream.of( container.getAsDouble() );
		}
		else {
			return Stream.empty();
		}
	}
}
