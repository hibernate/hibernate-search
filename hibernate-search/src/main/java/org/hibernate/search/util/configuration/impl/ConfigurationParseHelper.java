/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010-2011, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.util.configuration.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import org.hibernate.annotations.common.util.StringHelper;
import org.hibernate.search.SearchException;

/**
 * Helper class:
 * - to avoid managing NumberFormatException and similar code
 * - ensure consistent error messages across Configuration parsing problems
 * - locate resources
 * 
 * @author Sanne Grinovero
 * @author Steve Ebersole
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public abstract class ConfigurationParseHelper {

	/**
	 * Try to locate a local URL representing the incoming path. The first attempt
	 * assumes that the incoming path is an actual URL string (file://, etc).  If this
	 * does not work, then the next attempts try to locate this UURL as a java system
	 * resource.
	 *
	 * @param path The path representing the config location.
	 * @return An appropriate URL or null.
	 */
	public static URL locateConfig(final String path) {
		try {
			return new URL(path);
		}
		catch(MalformedURLException e) {
			return findAsResource(path);
		}
	}

	/**
	 * Try to locate a local URL representing the incoming path.
	 * This method <b>only</b> attempts to locate this URL as a
	 * java system resource.
	 *
	 * @param path The path representing the config location.
	 * @return An appropriate URL or null.
	 */
	public static URL findAsResource(final String path) {
		URL url = null;

		// First, try to locate this resource through the current
		// context classloader.
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		if (contextClassLoader!=null) {
			url = contextClassLoader.getResource(path);
		}
		if (url != null)
			return url;

		// Next, try to locate this resource through this class's classloader
		url = ConfigurationParseHelper.class.getClassLoader().getResource(path);
		if (url != null)
			return url;

		// Next, try to locate this resource through the system classloader
		url = ClassLoader.getSystemClassLoader().getResource(path);

		// Anywhere else we should look?
		return url;
	}
	
	/**
	 * Parses a String to get an int value.
	 * @param value A string containing an int value to parse
	 * @param errorMsgOnParseFailure message being wrapped in a SearchException if value is null or not correct.
	 * @return the parsed value
	 * @throws SearchException both for null values and for Strings not containing a valid int.
	 */
	public static final int parseInt(String value, String errorMsgOnParseFailure) {
		if ( value == null ) {
			throw new SearchException( errorMsgOnParseFailure );
		}
		else {
			try {
				return Integer.parseInt( value.trim() );
			} catch (NumberFormatException nfe) {
				throw new SearchException( errorMsgOnParseFailure, nfe );
			}
		}
	}
	
	/**
	 * In case value is null or an empty string the defValue is returned
	 * @param value
	 * @param defValue
	 * @param errorMsgOnParseFailure
	 * @return the converted int.
	 * @throws SearchException if value can't be parsed.
	 */
	public static final int parseInt(String value, int defValue, String errorMsgOnParseFailure) {
		if ( StringHelper.isEmpty( value ) ) {
			return defValue;
		}
		else {
			return parseInt( value, errorMsgOnParseFailure );
		}
	}
	
	/**
	 * Looks for a numeric value in the Properties, returning
	 * defValue if not found or if an empty string is found.
	 * When the key the value is found but not in valid format
	 * a standard error message is generated.
	 * @param cfg
	 * @param key
	 * @param defValue
	 * @return the converted int.
	 * @throws SearchException for invalid format.
	 */
	public static final int getIntValue(Properties cfg, String key, int defValue) {
		String propValue = cfg.getProperty( key );
		return parseInt( propValue, defValue, "Unable to parse " + key + ": " + propValue );
	}

	/**
	 * Parses a string to recognize exactly either "true" or "false".
	 * @param value the string to be parsed
	 * @param errorMsgOnParseFailure the message to be put in the exception if thrown
	 * @return true if value is "true", false if value is "false"
	 * @throws SearchException for invalid format or values.
	 */
	public static final boolean parseBoolean(String value, String errorMsgOnParseFailure) {
		// avoiding Boolean.valueOf() to have more checks: makes it easy to spot wrong type in cfg.
		if ( value == null ) {
			throw new SearchException( errorMsgOnParseFailure );
		}
		else if ( "false".equalsIgnoreCase( value.trim() ) ) {
			return false;
		}
		else if ( "true".equalsIgnoreCase( value.trim() ) ) {
			return true;
		}
		else {
			throw new SearchException( errorMsgOnParseFailure );
		}
	}

	/**
	 * Extracts a boolean value from configuration properties
	 * @param cfg configuration Properties
	 * @param key the property key
	 * @param defaultValue
	 * @return the defaultValue if the property was not defined
	 * @throws SearchException for invalid format or values.
	 */
	public static final boolean getBooleanValue(Properties cfg, String key, boolean defaultValue) {
		String propValue = cfg.getProperty( key );
		if ( propValue == null ) {
			return defaultValue;
		}
		else {
			return parseBoolean( propValue, "Property '" + key + "' needs to be either literal 'true' or 'false'" );
		}
	}

	/**
	 * Get the string property or defaults if not present
	 */
	public static final String getString(Properties cfg, String key, String defaultValue) {
		String propValue = cfg.getProperty( key );
		return propValue == null ? defaultValue : propValue;
	}
}
