/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.extractor.impl;

import java.util.Map;
import java.util.stream.Stream;

public class MapValueValueExtractor<T> implements ContainerValueExtractor<Map<?, T>, T> {
	private static final MapValueValueExtractor<?> INSTANCE = new MapValueValueExtractor();

	@SuppressWarnings( "unchecked" ) // INSTANCE works for any T
	public static <T> MapValueValueExtractor<T> get() {
		return (MapValueValueExtractor<T>) INSTANCE;
	}

	@Override
	public Stream<T> extract(Map<?, T> container) {
		return container == null ? Stream.empty() : container.values().stream();
	}
}
