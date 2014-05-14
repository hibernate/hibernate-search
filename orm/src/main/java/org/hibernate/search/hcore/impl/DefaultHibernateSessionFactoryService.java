/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.hcore.impl;

import java.util.Properties;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.service.spi.Startable;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * A Hibernate Search service which allows to request a Hibernate {@code SessionFactory} during bootstrapping.
 *
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
public class DefaultHibernateSessionFactoryService implements HibernateSessionFactoryService, Startable {
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
	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}
}
