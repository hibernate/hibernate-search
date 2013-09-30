/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.jmx.impl;

import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.hibernate.search.util.StringHelper;
import org.hibernate.search.SearchException;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Helper class to register JMX beans.
 *
 * @author Hardy Ferentschik
 */
public final class JMXRegistrar {
	private static final Log log = LoggerFactory.make();

	private JMXRegistrar() {
	}

	public static String buildMBeanName(String defaultName, String suffix) {
		String objectName = defaultName;
		if ( !StringHelper.isEmpty( suffix ) ) {
			objectName += "[" + suffix + "]";
		}
		return objectName;
	}

	/**
	 * Registers the specified object with the given name to the MBean server.
	 *
	 * @param object the object to register
	 * @param name the object name to register the bean under
	 *
	 * @return The registered object name
	 */
	public static String registerMBean(Object object, String name) {
		ObjectName objectName = createObjectName( name );
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		try {
			mbs.registerMBean( object, objectName );
		}
		catch (Exception e) {
			throw new SearchException( "Unable to enable MBean for Hibernate Search", e );
		}
		return objectName.toString();
	}

	/**
	 * Unregister the MBean with the specified name.
	 *
	 * @param name The name of the bean to unregister. The {@code name} cannot be {@code null}
	 *
	 * @throws java.lang.IllegalArgumentException
	 *          In case the object name is {@code null}
	 */
	public static void unRegisterMBean(String name) {
		if ( name == null ) {
			throw new IllegalArgumentException( "The object name cannot be null" );
		}
		ObjectName objectName = createObjectName( name );
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		if ( mbs.isRegistered( objectName ) ) {
			try {
				mbs.unregisterMBean( objectName );
			}
			catch (Exception e) {
				log.unableToUnregisterExistingMBean( name, e );
			}
		}
	}

	/**
	 * Checks whether a bean is registered under the given  name.
	 *
	 * @param name the object name to check (as string)
	 *
	 * @return {@code true} is there is a bean registered under the given name, {@code false} otherwise.
	 *
	 * @throws java.lang.IllegalArgumentException
	 *          In case the object name is {@code null}
	 */
	public static boolean isNameRegistered(String name) {
		if ( name == null ) {
			throw new IllegalArgumentException( "The object name cannot be null" );
		}
		ObjectName objectName = createObjectName( name );
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		return mbs.isRegistered( objectName );
	}

	private static ObjectName createObjectName(String name) {
		ObjectName objectName;
		try {
			objectName = new ObjectName( name );
		}
		catch (MalformedObjectNameException e) {
			throw new SearchException( "Invalid JMX Bean name: " + name, e );
		}
		return objectName;
	}
}


