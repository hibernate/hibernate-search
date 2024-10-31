/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import java.util.function.Supplier;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.mapper.orm.common.impl.HibernateOrmUtils;
import org.hibernate.search.mapper.orm.logging.impl.ConfigurationLog;
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
			throw ConfigurationLog.INSTANCE.hibernateSearchNotInitialized();
		}
	}

}
