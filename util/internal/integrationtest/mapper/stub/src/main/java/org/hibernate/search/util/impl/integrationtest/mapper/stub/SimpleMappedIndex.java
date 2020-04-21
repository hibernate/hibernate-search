/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.stub;

import java.util.function.Function;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingContext;

/**
 * An easy-to-use definition of a mapped index, including the backend name, index name, type name and binding.
 * <p>
 * Set up a mapped index by {@link StubMappingInitiator#add(StubMappedIndex) adding it to the initiator}.
 * The resulting binding will be made available through {@link #binding()} after startup,
 * and index manager methods are exposed directly on this class.
 */
public abstract class SimpleMappedIndex<B> extends StubMappedIndex {

	public static <B> SimpleMappedIndex<B> of(String indexName, Function<IndexSchemaElement, B> binder) {
		return new SimpleMappedIndex<B>( indexName ) {
			@Override
			protected B doBind(IndexedEntityBindingContext context) {
				return binder.apply( context.getSchemaElement() );
			}
		};
	}

	private B binding;

	protected SimpleMappedIndex(String indexName) {
		super( indexName );
	}

	public final B binding() {
		return binding;
	}

	@Override
	protected final void bind(IndexedEntityBindingContext context) {
		this.binding = doBind( context );
	}

	protected abstract B doBind(IndexedEntityBindingContext context);

}
