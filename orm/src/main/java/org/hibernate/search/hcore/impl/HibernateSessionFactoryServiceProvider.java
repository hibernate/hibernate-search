/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.hcore.impl;

import java.util.Properties;

import org.hibernate.SessionFactory;

import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.spi.ServiceProvider;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * A Hibernate Search service provider which allows to request a Hibernate {@code SessionFactory} during bootstrapping.
 *
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
public class HibernateSessionFactoryServiceProvider implements ServiceProvider<SessionFactory> {
	private static final Log log = LoggerFactory.make();

	private volatile SessionFactory sessionFactory;

	@Override
	public void start(Properties properties, BuildContext context) {
		if ( !properties.containsKey( HibernateSearchSessionFactoryObserver.SESSION_FACTORY_PROPERTY_KEY ) ) {
			throw log.getNoSessionFactoryInContextException();
		}
		sessionFactory = (SessionFactory) properties.get( HibernateSearchSessionFactoryObserver.SESSION_FACTORY_PROPERTY_KEY );
	}

	@Override
	public SessionFactory getService() {
		return sessionFactory;
	}

	@Override
	public void stop() {
		// no-op
	}
}
