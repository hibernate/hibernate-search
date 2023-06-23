/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.types.spi;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.engine.backend.metamodel.IndexObjectFieldTypeDescriptor;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.search.common.spi.SearchIndexCompositeNodeContext;
import org.hibernate.search.engine.search.common.spi.SearchIndexCompositeNodeTypeContext;
import org.hibernate.search.engine.search.common.spi.SearchIndexScope;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementFactory;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;

public abstract class AbstractIndexCompositeNodeType<
		SC extends SearchIndexScope<?>,
		N extends SearchIndexCompositeNodeContext<SC>>
		implements IndexObjectFieldTypeDescriptor, SearchIndexCompositeNodeTypeContext<SC, N> {
	private final ObjectStructure objectStructure;
	private final Map<SearchQueryElementTypeKey<?>, SearchQueryElementFactory<?, ? super SC, ? super N>> queryElementFactories;

	protected AbstractIndexCompositeNodeType(Builder<SC, N> builder) {
		this.objectStructure = builder.objectStructure;
		this.queryElementFactories = builder.queryElementFactories;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "objectStructure=" + objectStructure
				+ ", capabilities=" + queryElementFactories.keySet()
				+ "]";
	}

	@Override
	public boolean nested() {
		switch ( objectStructure ) {
			case NESTED:
				return true;
			case FLATTENED:
			case DEFAULT:
			default:
				return false;
		}
	}

	@SuppressWarnings("unchecked") // The cast is safe by construction; see the builder.
	@Override
	public final <T> SearchQueryElementFactory<? extends T, ? super SC, ? super N> queryElementFactory(
			SearchQueryElementTypeKey<T> key) {
		return (SearchQueryElementFactory<? extends T, ? super SC, ? super N>) queryElementFactories.get( key );
	}

	public abstract static class Builder<
			SC extends SearchIndexScope<?>,
			N extends SearchIndexCompositeNodeContext<SC>> {
		private final ObjectStructure objectStructure;
		private final Map<SearchQueryElementTypeKey<?>,
				SearchQueryElementFactory<?, ? super SC, ? super N>> queryElementFactories = new HashMap<>();

		public Builder(ObjectStructure objectStructure) {
			this.objectStructure = objectStructure;
		}

		public final <T> void queryElementFactory(SearchQueryElementTypeKey<T> key,
				SearchQueryElementFactory<? extends T, ? super SC, ? super N> factory) {
			queryElementFactories.put( key, factory );
		}

		public abstract AbstractIndexCompositeNodeType<SC, N> build();
	}
}
