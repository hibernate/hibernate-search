/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.mapping.impl;

import org.hibernate.search.mapper.pojo.standalone.loading.MassLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.loading.SelectionLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.loading.binding.EntityLoadingBinder;
import org.hibernate.search.mapper.pojo.standalone.loading.binding.EntityLoadingBindingContext;
import org.hibernate.search.mapper.pojo.standalone.mapping.metadata.EntityConfigurationContext;
import org.hibernate.search.mapper.pojo.standalone.mapping.metadata.EntityConfigurer;

@SuppressWarnings("deprecation")
public final class EntityConfigurerLoadingBinder<E> implements EntityLoadingBinder {

	private final Class<E> entityType;
	private final EntityConfigurer<E> delegate;

	public EntityConfigurerLoadingBinder(Class<E> entityType, EntityConfigurer<E> delegate) {
		this.entityType = entityType;
		this.delegate = delegate;
	}

	@Override
	public void bind(EntityLoadingBindingContext context) {
		delegate.configure( new EntityConfigurationContext<>() {
			@Override
			public void selectionLoadingStrategy(SelectionLoadingStrategy<? super E> strategy) {
				context.selectionLoadingStrategy( entityType, strategy );
			}

			@Override
			public void massLoadingStrategy(MassLoadingStrategy<? super E, ?> strategy) {
				context.massLoadingStrategy( entityType, strategy );
			}
		} );
	}
}
