/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.spi;

import java.util.List;

import org.hibernate.search.engine.search.projection.dsl.MultiProjectionTypeReference;

/**
 * A {@link ProjectionAccumulator} that can accumulate any number of values into a {@link List}.
 *
 * @param <E> The type of extracted values to accumulate before being transformed.
 * @param <V> The type of values to accumulate obtained by transforming extracted values ({@code E}).
 */
final class ListBasedProjectionAccumulator<C, E, V> extends AbstractListBasedProjectionAccumulator<E, V, C> {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	static <V, C> Provider<V, C> provider(MultiProjectionTypeReference<C, V> reference) {
		if ( MultiProjectionTypeReference.list().equals( reference ) ) {
			return LIST_PROVIDER;
		}
		if ( MultiProjectionTypeReference.set().equals( reference ) ) {
			return SET_PROVIDER;
		}
		if ( MultiProjectionTypeReference.sortedSet().equals( reference ) ) {
			return SORTED_SET_PROVIDER;
		}
		return new ListBasedProvider( reference );
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	static final Provider SET_PROVIDER = new ListBasedProvider( MultiProjectionTypeReference.set() );

	@SuppressWarnings({ "unchecked", "rawtypes" })
	static final Provider SORTED_SET_PROVIDER = new ListBasedProvider( MultiProjectionTypeReference.sortedSet() );

	@SuppressWarnings({ "unchecked", "rawtypes" })
	static final Provider LIST_PROVIDER = new ListBasedProvider( MultiProjectionTypeReference.list() );

	private final MultiProjectionTypeReference<C, V> reference;

	private ListBasedProjectionAccumulator(MultiProjectionTypeReference<C, V> reference) {
		this.reference = reference;
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public C finish(List<Object> accumulated) {
		// Hack to avoid instantiating another list: we convert a List<Object> into a List<U> just by replacing its elements.
		// It works *only* because we know the actual underlying type of the list,
		// and we know it can work just as well with U as with Object.
		return reference.convert( (List<V>) (List) accumulated );
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static class ListBasedProvider<V, R> implements Provider<V, R> {
		private final ListBasedProjectionAccumulator instance;

		private ListBasedProvider(MultiProjectionTypeReference<V, R> reference) {
			instance = new ListBasedProjectionAccumulator( reference );
		}

		@Override
		public ProjectionAccumulator get() {
			return instance;
		}

		@Override
		public boolean isSingleValued() {
			return false;
		}
	}
}
