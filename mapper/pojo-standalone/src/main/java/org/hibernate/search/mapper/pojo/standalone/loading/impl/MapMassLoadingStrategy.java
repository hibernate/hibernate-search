/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.loading.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.mapper.pojo.standalone.loading.LoadingTypeGroup;
import org.hibernate.search.mapper.pojo.standalone.loading.MassIdentifierLoader;
import org.hibernate.search.mapper.pojo.standalone.loading.MassIdentifierSink;
import org.hibernate.search.mapper.pojo.standalone.loading.MassEntityLoader;
import org.hibernate.search.mapper.pojo.standalone.loading.MassLoadingOptions;
import org.hibernate.search.mapper.pojo.standalone.loading.MassEntitySink;
import org.hibernate.search.mapper.pojo.standalone.loading.MassLoadingStrategy;

public final class MapMassLoadingStrategy<E, I> implements MassLoadingStrategy<E, I> {

	private final Map<I, E> source;

	public MapMassLoadingStrategy(Map<I, E> source) {
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
		MapMassLoadingStrategy<?, ?> that = (MapMassLoadingStrategy<?, ?>) o;
		return source.equals( that.source );
	}

	@Override
	public int hashCode() {
		return source.hashCode();
	}

	@Override
	public MassIdentifierLoader createIdentifierLoader(LoadingTypeGroup<E> includedTypes, MassIdentifierSink<I> sink,
			MassLoadingOptions options) {
		Set<I> identifiers = source.entrySet().stream()
				.filter( ent -> includedTypes.includesInstance( ent.getValue() ) )
				.map( Entry::getKey ).collect( Collectors.toSet() );
		Iterator<I> iterator = identifiers.iterator();
		return new MassIdentifierLoader() {
			@Override
			public void close() {
				// Nothing to do.
			}

			@Override
			public long totalCount() {
				return identifiers.size();
			}

			@Override
			public void loadNext() throws InterruptedException {
				int batchSize = options.batchSize();

				List<I> destination = new ArrayList<>( batchSize );
				while ( iterator.hasNext() && destination.size() < batchSize ) {
					destination.add( iterator.next() );
				}
				if ( destination.isEmpty() ) {
					sink.complete();
				}
				else {
					sink.accept( destination );
				}
			}
		};
	}

	@Override
	public MassEntityLoader<I> createEntityLoader(LoadingTypeGroup<E> includedTypes, MassEntitySink<E> sink,
			MassLoadingOptions options) {
		return new MassEntityLoader<I>() {
			@Override
			public void close() {
				// Nothing to do.
			}

			@Override
			public void load(List<I> identifiers) throws InterruptedException {
				sink.accept( identifiers.stream().map( source::get ).collect( Collectors.toList() ) );
			}
		};
	}

}
