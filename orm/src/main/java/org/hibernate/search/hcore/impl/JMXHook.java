/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.hcore.impl;

import static org.hibernate.engine.config.spi.StandardConverters.BOOLEAN;
import static org.hibernate.engine.config.spi.StandardConverters.STRING;

import java.util.Objects;

import org.hibernate.SessionFactory;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.jmx.IndexControlMBean;
import org.hibernate.search.orm.jmx.impl.IndexControl;
import org.hibernate.search.util.jmx.impl.JMXRegistrar;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Handles reading configuration properties about JMX MBeans
 * and registering (unregistering) the MBean as needed.
 */
final class JMXHook {

	private static final Log log = LoggerFactory.make();

	private final String indexControlMBeanName;
	private final boolean indexControlMBeanEnabled;

	//Guarded by synchronization
	private boolean registeredIfEnabled;

	JMXHook(ConfigurationService configurationService) {
		Boolean enableJMX = configurationService.getSetting( Environment.JMX_ENABLED, BOOLEAN, Boolean.FALSE );
		this.indexControlMBeanName = enableJMX.booleanValue() ? extractMBeanName( configurationService ) : null;
		this.indexControlMBeanEnabled = enableJMX.booleanValue() && indexControlMBeanName != null;
	}

	public synchronized void registerIfEnabled(ExtendedSearchIntegrator extendedIntegrator, SessionFactory factory) {
		Objects.requireNonNull( extendedIntegrator );
		Objects.requireNonNull( factory );
		if ( registeredIfEnabled ) {
			throw new AssertionFailure( "Unexpected state" );
		}
		if ( indexControlMBeanEnabled ) {
			enableIndexControlBean( indexControlMBeanName, extendedIntegrator, factory );
		}
		registeredIfEnabled = true;
	}

	public synchronized void unRegisterIfRegistered() {
		if ( registeredIfEnabled == false ) {
			throw new AssertionFailure( "Unexpected state" );
		}
		if ( registeredIfEnabled ) {
			JMXRegistrar.unRegisterMBean( indexControlMBeanName );
		}
		registeredIfEnabled = false;
	}

	private static String extractMBeanName(ConfigurationService configurationService) {
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
		return objectName;
	}

	private static String enableIndexControlBean(final String objectName, final ExtendedSearchIntegrator extendedIntegrator, final SessionFactory factory) {
		// since the SearchFactory is mutable we might have an already existing MBean which we have to unregister first
		if ( JMXRegistrar.isNameRegistered( objectName ) ) {
			JMXRegistrar.unRegisterMBean( objectName );
		}

		IndexControl indexCtrlBean = new IndexControl( extendedIntegrator, factory );
		JMXRegistrar.registerMBean( indexCtrlBean, IndexControlMBean.class, objectName );
		return objectName;
	}

}
