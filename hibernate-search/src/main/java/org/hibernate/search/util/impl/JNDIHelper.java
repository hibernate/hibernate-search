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
package org.hibernate.search.util.impl;

import java.util.HashSet;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Helper class for creating an JNDI {@code InitialContext}.
 *
 * @author Hardy Ferentschik
 */
public class JNDIHelper {

	public static final String HIBERNATE_JNDI_PREFIX = "hibernate.jndi.";

	private JNDIHelper() {
	}

	/**
	 * Creates an initial context
	 *
	 * @param properties Configuration properties to check for JNDI specific properties
	 * @param prefix The prefix used to designate JDNI properties. If a property from {@code property} contains
	 * a property which matches the prefix, the prefix gets removed and the property passed to the initial context creation.
	 *
	 * @return the initial context
	 *
	 * @throws NamingException in case an error occurs creating the {@code InitialContext}
	 */
	public static InitialContext getInitialContext(Properties properties, String prefix) throws NamingException {
		Properties jndiProps = getJndiProperties( properties, prefix );
		if ( jndiProps.size() == 0 ) {
			return new InitialContext();
		}
		else {
			return new InitialContext( jndiProps );
		}
	}

	public static Properties getJndiProperties(Properties properties, String prefix) {

		HashSet<String> specialProps = new HashSet<String>();
		specialProps.add( prefix + "class" );
		specialProps.add( prefix + "url" );

		Properties result = addJNDIProperties( properties, prefix, specialProps );

		handleSpecialPropertyTranslation( properties, prefix + "class", result, Context.INITIAL_CONTEXT_FACTORY );
		handleSpecialPropertyTranslation( properties, prefix + "url", result, Context.PROVIDER_URL );

		return result;
	}

	/**
	 * Creates a new {@code Properties} instance with all properties from {@code properties} which start with the given
	 *
	 * @param properties the original properties
	 * @param prefix the prefix indicating JNDI specific properties
	 * @param specialProps a set of property names to ignore
	 *
	 * @return Creates a new {@code Properties} instance with JNDI specific properties
	 *
	 * @{code prefix}. In the new instance the prefix is removed. If a property matches a value in {@code specialProps}
	 * it gets ignored.
	 */
	private static Properties addJNDIProperties(Properties properties, String prefix, HashSet<String> specialProps) {
		Properties result = new Properties();
		for ( Object property : properties.keySet() ) {
			if ( property instanceof String ) {
				String s = ( String ) property;
				if ( s.indexOf( prefix ) > -1 && !specialProps.contains( s ) ) {
					result.setProperty( s.substring( prefix.length() ), properties.getProperty( s ) );
				}
			}
		}
		return result;
	}

	private static void handleSpecialPropertyTranslation(Properties originalProperties, String oldKey, Properties newProperties, String newKey) {
		String value = originalProperties.getProperty( oldKey );
		// we want to be able to just use the defaults,
		// if JNDI environment properties are not supplied
		// so don't put null in anywhere
		if ( value != null ) {
			newProperties.put( newKey, value );
		}
	}
}


