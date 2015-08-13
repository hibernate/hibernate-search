/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.configuration.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import org.hibernate.search.util.StringHelper;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Helper class:
 * <ul>
 * <li>to avoid managing {@code NumberFormatException}s and similar</li>
 * <li>to ensure consistent error messages across configuration parsing</li>
 * <li>to locate resources</li>
 * </ul>
 *
 * @author Sanne Grinovero
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class ConfigurationParseHelper {
	private static final Log log = LoggerFactory.make();

	private ConfigurationParseHelper() {
	}

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
			return new URL( path );
		}
		catch (MalformedURLException e) {
			return findAsResource( path );
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
		if ( contextClassLoader != null ) {
			url = contextClassLoader.getResource( path );
		}
		if ( url != null ) {
			return url;
		}

		// Next, try to locate this resource through this class's classloader
		url = ConfigurationParseHelper.class.getClassLoader().getResource( path );
		if ( url != null ) {
			return url;
		}

		// Next, try to locate this resource through the system classloader
		url = ClassLoader.getSystemClassLoader().getResource( path );

		// Anywhere else we should look?
		return url;
	}

	/**
	 * Parses a string into an integer value.
	 *
	 * @param value a string containing an int value to parse
	 * @param errorMsgOnParseFailure message being wrapped in a SearchException if value is {@code null} or not an integer
	 *
	 * @return the parsed integer value
	 *
	 * @throws SearchException both for null values and for Strings not containing a valid int.
	 */
	public static int parseInt(String value, String errorMsgOnParseFailure) {
		if ( value == null ) {
			throw new SearchException( errorMsgOnParseFailure );
		}
		else {
			try {
				return Integer.parseInt( value.trim() );
			}
			catch (NumberFormatException nfe) {
				throw log.getInvalidIntegerValueException( errorMsgOnParseFailure, nfe );
			}
		}
	}

	/**
	 * Parses a String to get an long value.
	 *
	 * @param value A string containing an long value to parse
	 * @param errorMsgOnParseFailure message being wrapped in a SearchException if value is null or not correct.
	 * @return the parsed value
	 * @throws SearchException both for null values and for Strings not containing a valid int.
	 */
	public static long parseLong(String value, String errorMsgOnParseFailure) {
		if ( value == null ) {
			throw new SearchException( errorMsgOnParseFailure );
		}
		else {
			try {
				return Long.parseLong( value.trim() );
			}
			catch (NumberFormatException nfe) {
				throw new SearchException( errorMsgOnParseFailure, nfe );
			}
		}
	}

	/**
	 * In case value is null or an empty string the defValue is returned.
	 *
	 * @param value the text to parse
	 * @param defValue the value to return in case the text is null
 	 * @param errorMsgOnParseFailure message in case of error
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
	 * In case value is null or an empty string the defValue is returned
	 * @param value the text to parse
	 * @param defValue the value to return in case the text is null
 	 * @param errorMsgOnParseFailure message in case of error
	 * @return the converted long.
	 * @throws SearchException if value can't be parsed.
	 */
	public static final long parseLong(String value, long defValue, String errorMsgOnParseFailure) {
		if ( StringHelper.isEmpty( value ) ) {
			return defValue;
		}
		else {
			return parseLong( value, errorMsgOnParseFailure );
		}
	}

	/**
	 * Looks for a numeric value in the Properties, returning
	 * defValue if not found or if an empty string is found.
	 * When the key the value is found but not in valid format
	 * a standard error message is generated.
	 * @param cfg the properties
	 * @param key the property identifier
	 * @param defValue the value to return if the property is not found or empty
	 * @return the converted int.
	 * @throws SearchException for invalid format.
	 */
	public static final int getIntValue(Properties cfg, String key, int defValue) {
		String propValue = cfg.getProperty( key );
		return parseInt( propValue, defValue, "Unable to parse " + key + ": " + propValue );
	}

	/**
	 * Looks for a numeric value in the Properties, returning
	 * defValue if not found or if an empty string is found.
	 * When the key the value is found but not in valid format
	 * a standard error message is generated.
	 * @param cfg the properties
	 * @param key the property identifier
	 * @param defaultValue the value to return if the property is not found or empty
	 * @return the converted long value.
	 * @throws SearchException for invalid format.
	 */
	public static long getLongValue(Properties cfg, String key, long defaultValue) {
		String propValue = cfg.getProperty( key );
		return parseLong( propValue, defaultValue, "Unable to parse " + key + ": " + propValue );
	}

	/**
	 * Parses a string to recognize exactly either "true" or "false".
	 *
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
	 *
	 * @param cfg configuration Properties
	 * @param key the property key
	 * @param defaultValue a boolean.
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
	 * @param cfg configuration Properties
	 * @param key the property key
	 * @param defaultValue the value to return if the property value is null
	 * @return the String or default value
	 */
	public static final String getString(Properties cfg, String key, String defaultValue) {
		if ( cfg == null ) {
			return defaultValue;
		}
		else {
			String propValue = cfg.getProperty( key );
			return propValue == null ? defaultValue : propValue;
		}
	}

	/**
	 * Retrieves a configuration property and parses it as an Integer if it exists,
	 * or returns null if the property is not set (undefined).
	 * @param cfg configuration Properties
	 * @param key the property key
	 * @return the Integer or null
	 * @throws SearchException both for empty (non-null) values and for Strings not containing a valid int representation.
	 */
	public static Integer getIntValue(Properties cfg, String key) {
		String propValue = cfg.getProperty( key );
		if ( propValue == null ) {
			return null;
		}
		if ( StringHelper.isEmpty( propValue.trim() ) ) {
			throw log.configurationPropertyCantBeEmpty( key );
		}
		else {
			return parseInt( propValue, 0, "Unable to parse " + key + ": " + propValue );
		}
	}

}
