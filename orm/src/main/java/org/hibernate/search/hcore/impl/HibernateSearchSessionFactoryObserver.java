/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.hcore.impl;

import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.config.spi.ConfigurationService;
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

import static org.hibernate.engine.config.spi.StandardConverters.BOOLEAN;
import static org.hibernate.engine.config.spi.StandardConverters.STRING;

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

	private static final Log log = LoggerFactory.make();

	private final ConfigurationService configurationService;
	private final ClassLoaderService classLoaderService;
	private final FullTextIndexEventListener listener;
	private final Metadata metadata;

	private String indexControlMBeanName;
	private ExtendedSearchIntegrator extendedIntegrator;


	public HibernateSearchSessionFactoryObserver(
			Metadata metadata,
			ConfigurationService configurationService,
			FullTextIndexEventListener listener,
			ClassLoaderService classLoaderService) {
		this.metadata = metadata;
		this.configurationService = configurationService;
		this.listener = listener;
		this.classLoaderService = classLoaderService;
	}

	@Override
	public void sessionFactoryCreated(SessionFactory factory) {
		boolean failedBoot = true;
		try {
			final SessionFactoryImplementor factoryImplementor = (SessionFactoryImplementor) factory;
			HibernateSessionFactoryService sessionService = new DefaultHibernateSessionFactoryService( factory );
			if ( extendedIntegrator == null ) {
				SearchIntegrator searchIntegrator = new SearchIntegratorBuilder()
						.configuration( new SearchConfigurationFromHibernateCore( metadata, configurationService, classLoaderService, sessionService ) )
						.buildSearchIntegrator();
				extendedIntegrator = searchIntegrator.unwrap( ExtendedSearchIntegrator.class );
			}

			Boolean enableJMX = configurationService.getSetting( Environment.JMX_ENABLED, BOOLEAN, Boolean.FALSE );
			if ( enableJMX.booleanValue() ) {
				indexControlMBeanName =
						enableIndexControlBean( configurationService, extendedIntegrator );
			}
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

	private static String enableIndexControlBean(ConfigurationService configurationService, ExtendedSearchIntegrator extendedIntegrator) {
		// if we don't have a JNDI bound SessionFactory we cannot enable the index control bean
		if ( StringHelper.isEmpty( configurationService.getSetting( "hibernate.session_factory_name", STRING ) ) ) {
			log.debug( "In order to bind the IndexControlMBean the Hibernate SessionFactory has to be available via JNDI" );
			return null;
		}

		String mbeanNameSuffix = configurationService.getSetting( Environment.JMX_BEAN_SUFFIX, STRING );
		String objectName = JMXRegistrar.buildMBeanName(
				IndexControl.INDEX_CTRL_MBEAN_OBJECT_NAME,
				mbeanNameSuffix
		);

		// since the SearchFactory is mutable we might have an already existing MBean which we have to unregister first
		if ( JMXRegistrar.isNameRegistered( objectName ) ) {
			JMXRegistrar.unRegisterMBean( objectName );
		}

		IndexControl indexCtrlBean = new IndexControl( configurationService, extendedIntegrator );
		JMXRegistrar.registerMBean( indexCtrlBean, IndexControlMBean.class, objectName );
		return objectName;
	}
}


