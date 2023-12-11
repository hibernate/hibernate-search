/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.types.spi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.search.engine.backend.metamodel.IndexFieldTypeDescriptor;
import org.hibernate.search.engine.search.common.spi.SearchIndexNodeContext;
import org.hibernate.search.engine.search.common.spi.SearchIndexNodeTypeContext;
import org.hibernate.search.engine.search.common.spi.SearchIndexScope;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementFactory;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;

public abstract class AbstractIndexNodeType<
		SC extends SearchIndexScope<?>,
		N extends SearchIndexNodeContext<SC>>
		implements IndexFieldTypeDescriptor, SearchIndexNodeTypeContext<SC, N> {
	private final Map<SearchQueryElementTypeKey<?>, SearchQueryElementFactory<?, ? super SC, ? super N>> queryElementFactories;
	private final Set<String> traits;

	protected AbstractIndexNodeType(Builder<SC, N> builder) {
		this.queryElementFactories = builder.queryElementFactories;
		TreeSet<String> modifiableTraits = new TreeSet<>();
		for ( SearchQueryElementTypeKey<?> searchQueryElementTypeKey : queryElementFactories.keySet() ) {
			String name = searchQueryElementTypeKey.name();
			modifiableTraits.add( name );
		}
		this.traits = Collections.unmodifiableSet( modifiableTraits );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ ", traits=" + traits
				+ "]";
	}

	@Override
	public Set<String> traits() {
		return traits;
	}

	@SuppressWarnings("unchecked") // The cast is safe by construction; see the builder.
	@Override
	public final <T> SearchQueryElementFactory<? extends T, ? super SC, ? super N> queryElementFactory(
			SearchQueryElementTypeKey<T> key) {
		return (SearchQueryElementFactory<? extends T, ? super SC, ? super N>) queryElementFactories.get( key );
	}

	public abstract static class Builder<
			SC extends SearchIndexScope<?>,
			N extends SearchIndexNodeContext<SC>> {
		private final Map<SearchQueryElementTypeKey<?>,
				SearchQueryElementFactory<?, ? super SC, ? super N>> queryElementFactories = new HashMap<>();

		protected Builder() {
		}

		public final <T> void queryElementFactory(SearchQueryElementTypeKey<T> key,
				SearchQueryElementFactory<? extends T, ? super SC, ? super N> factory) {
			queryElementFactories.put( key, factory );
		}

		public abstract AbstractIndexNodeType<SC, N> build();
	}
}
