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
package org.hibernate.search.test.util;

import java.util.Properties;

import org.slf4j.Logger;

/**
 * JGroups tests require some system properties to be set; while this can be done via Maven
 * that doesn't apply to other tools to we explicitly set the environment parameters.
 * 
 * FIXME this doesn't seem to work, java.net.preferIPv4Stack is applied but ignored by JGroups,
 * while it works fine if setting the property as a JVM parameter
 */
public class JGroupsEnvironment {
	
	private static final Logger log = org.hibernate.search.util.LoggerFactory.make();
	
	// some system properties needed for JGroups
	static {
		Properties properties = System.getProperties();

		// Following is the default jgroups mcast address. If you find the testsuite runs very slowly,
		// there may be problems with multicast on the interface JGroups uses by default on
		// your machine. You can try to resolve setting 'jgroups.bind_addr' as a system-property
		// to the jvm launching maven and setting the value to an interface where you know multicast works
		String ip4Stack = "java.net.preferIPv4Stack";
		if ( properties.containsKey( ip4Stack ) ) {
			log.debug( "Found explicit value for '" + ip4Stack + "' Using value: " + properties.get( ip4Stack ) );
		}
		else {
			log.debug( "'" + ip4Stack + "' property not set. Setting it explicitly to 'true'" );
			System.setProperty( ip4Stack, "true" );
		}

		// There are problems with multicast and IPv6 on some OS/JDK combos, so we tell Java
		// to use IPv4. If you have problems with multicast when running the tests you can
		// try setting this to 'false', although typically that won't be helpful.
		String bindAddress = "jgroups.bind_addr";
		if ( properties.containsKey( bindAddress ) ) {
			log.debug( "Found explicit value for '" + bindAddress + "' Using value: " + properties.get( bindAddress ) );
		}
		else {
			log.debug( "'" + bindAddress + "' property not set. Setting it explicitly to '127.0.0.1'" );
			System.setProperty( "jgroups.bind_addr", "127.0.0.1" );
		}
	}
	
	public static void initJGroupsProperties() {
		log.debug( "JGroups environment setup" );
	}

}
