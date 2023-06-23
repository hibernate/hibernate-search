/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import java.lang.invoke.MethodHandles;
import java.util.function.Supplier;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.mapper.orm.common.impl.HibernateOrmUtils;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.service.Service;

/**
 * A Hibernate ORM service providing context to Hibernate Search components
 * when all they have access to is a Hibernate ORM session factory or session.
 */
public final class HibernateSearchContextProviderService
		implements Service, AutoCloseable, Supplier<HibernateOrmMapping> {

	public static HibernateSearchContextProviderService get(SessionFactoryImplementor sessionFactory) {
		return HibernateOrmUtils.getServiceOrFail( sessionFactory.getServiceRegistry(),
				HibernateSearchContextProviderService.class );
	}

	private volatile HibernateOrmMapping mapping;

	@Override
	public void close() {
		if ( mapping != null ) {
			mapping.close();
		}
	}

	public void initialize(HibernateOrmMapping mapping) {
		this.mapping = mapping;
	}

	@Override
	public HibernateOrmMapping get() {
		if ( mapping != null ) {
			return mapping;
		}
		else {
			throw LoggerFactory.make( Log.class, MethodHandles.lookup() ).hibernateSearchNotInitialized();
		}
	}

}
