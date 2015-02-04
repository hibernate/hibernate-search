/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.hcore.impl;

import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.cfg.impl.SearchConfigurationFromHibernateCore;
import org.hibernate.search.engine.Version;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.event.impl.FullTextIndexEventListener;
import org.hibernate.search.jmx.IndexControlMBean;
import org.hibernate.search.jmx.impl.IndexControl;
import org.hibernate.search.jmx.impl.JMXRegistrar;
import org.hibernate.search.spi.SearchIntegratorBuilder;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.util.StringHelper;
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
	private ClassLoaderService classLoaderService;
	private final FullTextIndexEventListener listener;

	private String indexControlMBeanName;
	private ExtendedSearchIntegrator extendedIntegrator;

	public HibernateSearchSessionFactoryObserver(Configuration configuration,
			FullTextIndexEventListener listener,
			ClassLoaderService classLoaderService) {
		this.configuration = configuration;
		this.listener = listener;
		this.classLoaderService = classLoaderService;
	}

	@Override
	public void sessionFactoryCreated(SessionFactory factory) {
		boolean failedBoot = true;
		try {
			final SessionFactoryImplementor factoryImplementor = (SessionFactoryImplementor) factory;
			configuration.getProperties().put( SESSION_FACTORY_PROPERTY_KEY, factory );
			if ( extendedIntegrator == null ) {
				SearchIntegrator searchIntegrator = new SearchIntegratorBuilder()
						.configuration( new SearchConfigurationFromHibernateCore( configuration, classLoaderService ) )
						.buildSearchIntegrator();
				extendedIntegrator = searchIntegrator.unwrap( ExtendedSearchIntegrator.class );
			}

			String enableJMX = configuration.getProperty( Environment.JMX_ENABLED );
			if ( "true".equalsIgnoreCase( enableJMX ) ) {
				enableIndexControlBean();
			}
			configuration = null; //free up some memory as we no longer need it
			listener.initialize( extendedIntegrator );
			//Register the SearchFactory in the ORM ServiceRegistry (for convenience of lookup)
			factoryImplementor.getServiceRegistry().getService( SearchFactoryReference.class ).initialize( extendedIntegrator );
			failedBoot = false;
		}
		finally {
			if ( failedBoot ) {
				factory.close();
			}
		}
	}

	@Override
	public void sessionFactoryClosed(SessionFactory factory) {
		if ( extendedIntegrator != null ) {
			extendedIntegrator.close();
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

		IndexControl indexCtrlBean = new IndexControl(
				configuration.getProperties(),
				extendedIntegrator.getServiceManager()
		);
		JMXRegistrar.registerMBean( indexCtrlBean, IndexControlMBean.class, objectName );
		indexControlMBeanName = objectName;
	}
}


