/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.mapping.metadata.impl;

import java.util.Optional;

import org.hibernate.search.mapper.pojo.standalone.loading.MassLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.loading.SelectionLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.mapping.metadata.EntityConfigurationContext;

public class StandalonePojoEntityTypeMetadata<E> {
	public final String name;
	public final Optional<SelectionLoadingStrategy<? super E>> selectionLoadingStrategy;
	public final Optional<MassLoadingStrategy<? super E, ?>> massLoadingStrategy;

	private StandalonePojoEntityTypeMetadata(String name, Builder<E> builder) {
		this.name = name;
		this.selectionLoadingStrategy = Optional.ofNullable( builder.selectionLoadingStrategy );
		this.massLoadingStrategy = Optional.ofNullable( builder.massLoadingStrategy );
	}

	public static class Builder<E> implements EntityConfigurationContext<E> {
		private final String name;
		private SelectionLoadingStrategy<? super E> selectionLoadingStrategy;
		private MassLoadingStrategy<? super E, ?> massLoadingStrategy;

		public Builder(String name) {
			this.name = name;
		}

		@Override
		public void selectionLoadingStrategy(SelectionLoadingStrategy<? super E> strategy) {
			this.selectionLoadingStrategy = strategy;
		}

		@Override
		public void massLoadingStrategy(MassLoadingStrategy<? super E, ?> strategy) {
			this.massLoadingStrategy = strategy;
		}

		public StandalonePojoEntityTypeMetadata<E> build() {
			return new StandalonePojoEntityTypeMetadata<>( name, this );
		}
	}
}