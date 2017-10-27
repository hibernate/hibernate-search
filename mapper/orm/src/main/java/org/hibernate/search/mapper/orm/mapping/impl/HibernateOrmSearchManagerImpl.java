/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import java.util.Collection;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.mapper.orm.hibernate.HibernateOrmSearchResultDefinitionContext;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchManager;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchManagerBuilder;
import org.hibernate.search.mapper.orm.model.impl.HibernateOrmProxyIntrospector;
import org.hibernate.search.mapper.orm.search.impl.HibernateOrmSearchResultDefinitionContextImpl;
import org.hibernate.search.mapper.orm.search.impl.MutableObjectLoadingOptions;
import org.hibernate.search.mapper.orm.search.impl.ObjectLoaderBuilder;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoSearchManagerImpl;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoSearchTarget;
import org.hibernate.search.mapper.pojo.model.spi.PojoProxyIntrospector;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.engine.search.dsl.SearchResultDefinitionContext;

class HibernateOrmSearchManagerImpl extends PojoSearchManagerImpl
		implements HibernateOrmSearchManager {
	private final SessionImplementor sessionImplementor;

	private HibernateOrmSearchManagerImpl(Builder builder) {
		super( builder );
		this.sessionImplementor = builder.sessionImplementor;
	}

	@Override
	public <T> SearchResultDefinitionContext<PojoReference, T> search(Collection<? extends Class<? extends T>> targetedTypes) {
		return searchAsSearchQuery( targetedTypes );
	}

	@Override
	public <T> HibernateOrmSearchResultDefinitionContext<T> searchAsFullTextQuery(
			Collection<? extends Class<? extends T>> types) {
		return new HibernateOrmSearchResultDefinitionContextImpl<>(
				getMappingDelegate().createPojoSearchTarget( types ),
				getSessionContext(),
				sessionImplementor
		);
	}

	private <T> SearchResultDefinitionContext<PojoReference, T> searchAsSearchQuery(
			Collection<? extends Class<? extends T>> targetedTypes) {
		PojoSearchTarget<T> searchTarget = getMappingDelegate().createPojoSearchTarget( targetedTypes );
		ObjectLoaderBuilder<T> objectLoaderBuilder = new ObjectLoaderBuilder<>(
				sessionImplementor,
				searchTarget.getTargetedIndexedTypes()
		);
		MutableObjectLoadingOptions loadingOptions = new MutableObjectLoadingOptions();
		return searchTarget.search( getSessionContext(), objectLoaderBuilder.build( loadingOptions ) );
	}

	static class Builder extends PojoSearchManagerImpl.Builder<HibernateOrmSearchManager>
			implements HibernateOrmSearchManagerBuilder {
		private final SessionImplementor sessionImplementor;

		public Builder(PojoMappingDelegate mappingDelegate, SessionImplementor sessionImplementor) {
			super( mappingDelegate );
			this.sessionImplementor = sessionImplementor;
		}

		@Override
		protected String getTenantId() {
			return sessionImplementor.getTenantIdentifier();
		}

		@Override
		protected PojoProxyIntrospector getProxyIntrospector() {
			return new HibernateOrmProxyIntrospector( sessionImplementor );
		}

		@Override
		public HibernateOrmSearchManager build() {
			return new HibernateOrmSearchManagerImpl( this );
		}
	}
}
