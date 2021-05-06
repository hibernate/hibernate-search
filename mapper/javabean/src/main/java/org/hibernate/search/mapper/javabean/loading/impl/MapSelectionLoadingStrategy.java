/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.loading.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.search.engine.common.timing.Deadline;
import org.hibernate.search.mapper.javabean.loading.LoadingTypeGroup;
import org.hibernate.search.mapper.javabean.loading.SelectionEntityLoader;
import org.hibernate.search.mapper.javabean.loading.SelectionLoadingOptions;
import org.hibernate.search.mapper.javabean.loading.SelectionLoadingStrategy;

public class MapSelectionLoadingStrategy<E, I> implements SelectionLoadingStrategy<E> {

	private final Map<I, E> source;

	public MapSelectionLoadingStrategy(Map<I, E> source) {
		this.source = source;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		MapSelectionLoadingStrategy<?, ?> that = (MapSelectionLoadingStrategy<?, ?>) o;
		return source.equals( that.source );
	}

	@Override
	public int hashCode() {
		return source.hashCode();
	}

	@Override
	public SelectionEntityLoader<E> createEntityLoader(LoadingTypeGroup<E> includedTypes,
			SelectionLoadingOptions options) {
		return new SelectionEntityLoader<E>() {
			@Override
			public List<E> load(List<?> identifiers, Deadline deadline) {
				return identifiers.stream().map( source::get ).collect( Collectors.toList() );
			}
		};
	}

}
