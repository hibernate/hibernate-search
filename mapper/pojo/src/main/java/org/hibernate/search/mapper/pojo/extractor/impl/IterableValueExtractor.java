/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.extractor.impl;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class IterableValueExtractor<T> implements ContainerValueExtractor<Iterable<T>, T> {
	private static final IterableValueExtractor<?> INSTANCE = new IterableValueExtractor();

	@SuppressWarnings( "unchecked" ) // INSTANCE works for any T
	public static <T> IterableValueExtractor<T> get() {
		return (IterableValueExtractor<T>) INSTANCE;
	}

	@Override
	public Stream<T> extract(Iterable<T> container) {
		return container == null ? Stream.empty() : StreamSupport.stream( container.spliterator(), false );
	}
}
