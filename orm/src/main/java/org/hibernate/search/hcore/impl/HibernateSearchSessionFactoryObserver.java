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

import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;

import org.hibernate.search.util.StringHelper;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.Environment;
import org.hibernate.search.Version;
import org.hibernate.search.cfg.impl.SearchConfigurationFromHibernateCore;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.event.impl.FullTextIndexEventListener;
import org.hibernate.search.jmx.IndexControl;
import org.hibernate.search.jmx.impl.JMXRegistrar;
import org.hibernate.search.spi.SearchFactoryBuilder;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * A {@code SessionFactoryObserver} registered with Hibernate ORM during the integration phase. This observer will
 * create the Search factory once the {@code SessionFactory} is built.
 *
 * @author Hardy Ferentschik
 * @see HibernateSearchIntegrator
 */
public class HibernateSearchSessionFactoryObserver implements SessionFactoryObserver {
	static {
		Version.touch();
	}

	public static final String SESSION_FACTORY_PROPERTY_KEY = "hibernate.search.hcore.session_factory";

	private static final Log log = LoggerFactory.make();

	private Configuration configuration;
	private final FullTextIndexEventListener listener;

	private String indexControlMBeanName;
	private SearchFactoryImplementor searchFactoryImplementor;

	public HibernateSearchSessionFactoryObserver(Configuration configuration, FullTextIndexEventListener listener) {
		this.configuration = configuration;
		this.listener = listener;
	}

	@Override
	public void sessionFactoryCreated(SessionFactory factory) {
		try {
			configuration.getProperties().put( SESSION_FACTORY_PROPERTY_KEY, factory );
			if ( searchFactoryImplementor == null ) {
				searchFactoryImplementor = new SearchFactoryBuilder()
						.configuration( new SearchConfigurationFromHibernateCore( configuration ) )
						.buildSearchFactory();
			}

			String enableJMX = configuration.getProperty( Environment.JMX_ENABLED );
			if ( "true".equalsIgnoreCase( enableJMX ) ) {
				enableIndexControlBean();
			}
			configuration = null; //free up some memory as we no longer need it
			listener.initialize( searchFactoryImplementor );
		}
		catch (RuntimeException e) {
			factory.close();
			throw e;
		}
	}

	@Override
	public void sessionFactoryClosed(SessionFactory factory) {
		if ( searchFactoryImplementor != null ) {
			searchFactoryImplementor.close();
		}
		if ( indexControlMBeanName != null ) {
			JMXRegistrar.unRegisterMBean( indexControlMBeanName );
		}
	}

	private void enableIndexControlBean() {
		// if we don't have a JNDI bound SessionFactory we cannot enable the index control bean
		if ( StringHelper.isEmpty( configuration.getProperty( "hibernate.session_factory_name" ) ) ) {
			log.debug(
					"In order to bind the IndexControlMBean the Hibernate SessionFactory has to be available via JNDI"
			);
			return;
		}

		String mbeanNameSuffix = configuration.getProperty( Environment.JMX_BEAN_SUFFIX );
		String objectName = JMXRegistrar.buildMBeanName(
				IndexControl.INDEX_CTRL_MBEAN_OBJECT_NAME,
				mbeanNameSuffix
		);

		// since the SearchFactory is mutable we might have an already existing MBean which we have to unregister first
		if ( JMXRegistrar.isNameRegistered( objectName ) ) {
			JMXRegistrar.unRegisterMBean( objectName );
		}

		IndexControl indexCtrlBean = new IndexControl( configuration.getProperties() );
		JMXRegistrar.registerMBean( indexCtrlBean, objectName );
		indexControlMBeanName = objectName;
	}
}


