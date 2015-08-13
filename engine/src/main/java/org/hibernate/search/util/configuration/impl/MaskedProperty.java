/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.configuration.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * A wrapper to {@link Properties}, to restrict the availability of values to only those which have a key
 * beginning with a given masking string.
 *
 * Supported methods to enumerate the list of properties are:
 * <ul>
 * <li>propertyNames()</li>
 * <li>keySet()</li>
 * <li>keys()</li>
 * </ul>
 * Other methods including methods returning Entries and values are not supported
 *
 * @author Sanne Grinovero
 * @author Emmanuel Bernard
 */
public class MaskedProperty extends Properties implements Serializable {

	private static final long serialVersionUID = -593307257383085113L;
	private static final Log log = LoggerFactory.make();

	private final Properties masked;
	private final Properties fallBack;
	private final String radix;
	private transient Set<Object> propertyNames;

	/**
	 * Provides a view to the provided Properties hiding
	 * all keys not starting with some [mask.].
	 *
	 * @param propsToMask the Properties containing the values.
	 * @param mask a {@link java.lang.String} object.
	 */
	public MaskedProperty(Properties propsToMask, String mask) {
		this( propsToMask, mask, null );
	}

	/**
	 * Provides a view to the provided Properties hiding all keys not starting with some [mask.].
	 * If no value is found then a value is returned from propsFallBack, without masking.
	 *
	 * @param propsToMask the properties to mask
	 * @param mask the mask applied to the properties
	 * @param propsFallBack a fall-back map of properties in case a value is not found in the main one
	 */
	public MaskedProperty(Properties propsToMask, String mask, Properties propsFallBack) {
		if ( propsToMask == null || mask == null ) {
			throw new java.lang.IllegalArgumentException();
		}
		this.masked = propsToMask;
		this.radix = mask + ".";
		this.fallBack = propsFallBack;
	}

	@Override
	public String getProperty(String key) {
		String compositeKey = radix + key;
		String value = masked.getProperty( compositeKey );
		if ( value != null ) {
			log.tracef( "found a match for key: [%s] value: %s", compositeKey, value );
			return value;
		}
		else if ( fallBack != null ) {
			return fallBack.getProperty( key );
		}
		else {
			return null;
		}
	}

	/**
	 * Check if a given properties is set.
	 *
	 * @param key the property key
	 * @return {@code true} if the the property is set, {@code false} otherwise
	 * @throws IllegalArgumentException if the key is not a String instance
	 */
	@Override
	public synchronized boolean containsKey(Object key) {
		if ( !( key instanceof String ) ) {
			throw new IllegalArgumentException( "key must be a String" );
		}
		return getProperty( key.toString() ) != null;
	}

	@Override
	public String getProperty(String key, String defaultValue) {
		String val = getProperty( key );
		return ( val == null ) ? defaultValue : val;
	}

	/**
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void list(PrintStream out) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void list(PrintWriter out) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void load(InputStream inStream) throws IOException {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void loadFromXML(InputStream in) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized Enumeration<?> propertyNames() {
		initPropertyNames();
		return Collections.enumeration( propertyNames );
	}

	private synchronized void initPropertyNames() {
		if ( propertyNames != null ) {
			return;
		}
		Set<Object> maskedProperties = new TreeSet<Object>();
		//we use keys to be safe and avoid CCE for non String key entries
		Enumeration<?> maskedNames = masked.propertyNames();
		while ( maskedNames.hasMoreElements() ) {
			Object key = maskedNames.nextElement();
			if ( String.class.isInstance( key ) ) {
				String maskedProperty = (String) key;
				if ( maskedProperty.startsWith( radix ) ) {
					maskedProperties.add( maskedProperty.substring( radix.length(), maskedProperty.length() ) );
				}
			}
		}
		if ( fallBack != null ) {
			Enumeration<?> fallBackNames = fallBack.propertyNames();
			while ( fallBackNames.hasMoreElements() ) {
				Object key = fallBackNames.nextElement();
				if ( String.class.isInstance( key ) ) {
					maskedProperties.add( key );
				}
			}
		}
		propertyNames = Collections.unmodifiableSet( maskedProperties );
	}

	/**
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void save(OutputStream out, String comments) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public Object setProperty(String key, String value) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void store(OutputStream out, String comments)
			throws IOException {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void storeToXML(OutputStream os, String comment,
			String encoding) throws IOException {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void storeToXML(OutputStream os, String comment)
			throws IOException {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public Object clone() {
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized boolean contains(Object value) {
		initPropertyNames();
		return propertyNames.contains( value );
	}

	/**
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public Enumeration<Object> elements() {
		//TODO
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public Set<java.util.Map.Entry<Object, Object>> entrySet() {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public Object get(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized boolean isEmpty() {
		initPropertyNames();
		return propertyNames.isEmpty();
	}

	/**
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public synchronized Enumeration<Object> keys() {
		initPropertyNames();
		return Collections.enumeration( propertyNames );
	}

	@Override
	public synchronized Set<Object> keySet() {
		initPropertyNames();
		return propertyNames;
	}

	/**
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public Object put(Object key, Object value) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void putAll(Map<? extends Object, ? extends Object> t) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException always
	 */
	@Override
	protected void rehash() {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public Object remove(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized int size() {
		initPropertyNames();
		return propertyNames.size();
	}

	@Override
	public synchronized String toString() {
		HashMap fake = new HashMap();
		Enumeration<?> names = propertyNames();
		while ( names.hasMoreElements() ) {
			Object nextElement = names.nextElement();
			fake.put( nextElement, this.getProperty( nextElement.toString() ) );
		}
		return fake.toString();
	}

	/**
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public Collection<Object> values() {
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized int hashCode() {
		final int prime = 31;
		int result = ( ( fallBack == null ) ? 0 : fallBack.hashCode() );
		result = prime * result + masked.hashCode();
		result = prime * result + radix.hashCode();
		return result;
	}

	@Override
	public synchronized boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		final MaskedProperty other = (MaskedProperty) obj;
		if ( fallBack == null ) {
			if ( other.fallBack != null ) {
				return false;
			}
		}
		else if ( !fallBack.equals( other.fallBack ) ) {
			return false;
		}
		if ( !masked.equals( other.masked ) ) {
			return false;
		}
		if ( !radix.equals( other.radix ) ) {
			return false;
		}
		return true;
	}

}
