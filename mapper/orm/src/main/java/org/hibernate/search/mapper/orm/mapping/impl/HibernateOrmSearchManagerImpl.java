/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import java.util.Collection;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.mapper.orm.hibernate.HibernateOrmSearchTarget;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchManager;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchManagerBuilder;
import org.hibernate.search.mapper.orm.search.impl.HibernateOrmSearchTargetImpl;
import org.hibernate.search.mapper.orm.session.context.impl.HibernateOrmSessionContextImpl;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoSearchManagerImpl;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoSearchTargetDelegate;
import org.hibernate.search.mapper.pojo.session.context.spi.PojoSessionContextImplementor;

class HibernateOrmSearchManagerImpl extends PojoSearchManagerImpl
		implements HibernateOrmSearchManager {
	private final SessionImplementor sessionImplementor;

	private HibernateOrmSearchManagerImpl(Builder builder) {
		super( builder );
		this.sessionImplementor = builder.sessionImplementor;
	}

	@Override
	public <T> HibernateOrmSearchTarget<T> search(Collection<? extends Class<? extends T>> targetedTypes) {
		PojoSearchTargetDelegate<T> searchTargetDelegate = getMappingDelegate()
				.createPojoSearchTarget( targetedTypes, getSessionContext() );
		return new HibernateOrmSearchTargetImpl<>( searchTargetDelegate, sessionImplementor );
	}

	static class Builder extends AbstractBuilder<HibernateOrmSearchManager>
			implements HibernateOrmSearchManagerBuilder {
		private final SessionImplementor sessionImplementor;

		public Builder(PojoMappingDelegate mappingDelegate, SessionImplementor sessionImplementor) {
			super( mappingDelegate );
			this.sessionImplementor = sessionImplementor;
		}

		@Override
		protected PojoSessionContextImplementor buildSessionContext() {
			return new HibernateOrmSessionContextImpl( sessionImplementor );
		}

		@Override
		public HibernateOrmSearchManager build() {
			return new HibernateOrmSearchManagerImpl( this );
		}
	}
}
