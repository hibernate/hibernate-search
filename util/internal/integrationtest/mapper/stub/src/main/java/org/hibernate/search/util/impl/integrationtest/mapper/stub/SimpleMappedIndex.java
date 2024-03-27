/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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

	public static <B> SimpleMappedIndex<B> of(Function<IndexSchemaElement, B> binder) {
		return ofAdvanced( ctx -> binder.apply( ctx.schemaElement() ) );
	}

	public static <B> SimpleMappedIndex<B> ofAdvanced(Function<IndexedEntityBindingContext, B> binder) {
		return new SimpleMappedIndex<B>() {
			@Override
			protected B doBind(IndexedEntityBindingContext context) {
				return binder.apply( context );
			}
		};
	}

	private B binding;

	protected SimpleMappedIndex() {
	}

	@Override
	public SimpleMappedIndex<B> name(String name) {
		super.name( name );
		return this;
	}

	@Override
	public StubMappedIndex typeName(String name) {
		super.typeName( name );
		return this;
	}

	@Override
	public SimpleMappedIndex<B> backendName(String name) {
		super.backendName( name );
		return this;
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
