// $Id$
package org.hibernate.search.backend.configuration;

import java.util.Properties;

import org.hibernate.util.StringHelper;
import org.hibernate.search.SearchException;

/**
 * Helper class to avoid managing NumberFormatException and similar code
 * and ensure consistent error messages across Configuration parsing problems.
 * 
 * @author Sanne Grinovero
 */
public abstract class ConfigurationParseHelper {
	
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

}
