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
import java.util.InvalidPropertiesFormatException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * A wrapper to Properties, to restrict the availability of
 * values to only those which have a key beginning with some
 * masking String.
 * Supported methods to enumerate the list of properties are:
 *   - propertyNames()
 *   - keySet()
 *   - keys()
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
	 * @param propsToMask the Properties containing the values.
	 * @param mask
	 */
	public MaskedProperty(Properties propsToMask, String mask) {
		this( propsToMask, mask, null );
	}
	
	/**
	 * Provides a view to the provided Properties hiding
	 * all keys not starting with some [mask.].
	 * If no value is found then a value is returned from propsFallBack,
	 * without masking.
	 * @param propsToMask
	 * @param mask
	 * @param propsFallBack
	 */
	public MaskedProperty(Properties propsToMask, String mask, Properties propsFallBack) {
		if ( propsToMask==null || mask==null ) {
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
		if ( value != null) {
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
	 * @throws IllegalArgumentException if the key is not a String instance
	 */
	@Override
	public synchronized boolean containsKey(Object key) {
		if ( ! ( key instanceof String ) ) {
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
	 * @throws UnsupportedOperationException
	 */
	@Override
	public void list(PrintStream out) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException
	 */
	@Override
	public void list(PrintWriter out) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException
	 */
	@Override
	public void load(InputStream inStream) throws IOException {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException
	 */
	@Override
	public void loadFromXML(InputStream in) throws IOException,
			InvalidPropertiesFormatException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Enumeration<?> propertyNames() {
		initPropertyNames();
		return Collections.enumeration( propertyNames );
	}

	private synchronized void initPropertyNames() {
		if ( propertyNames != null) return;
		Set<Object> maskedProperties = new TreeSet<Object>();
		//we use keys to be safe and avoid CCE for non String key entries
		Enumeration<?> maskedNames = masked.propertyNames();
		while ( maskedNames.hasMoreElements() ) {
			Object key = maskedNames.nextElement();
			if ( String.class.isInstance( key ) ) {
				String maskedProperty = (String) key;
				if ( maskedProperty.startsWith( radix ) ) {
					maskedProperties.add(maskedProperty.substring( radix.length(), maskedProperty.length() ) );
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
	 * @throws UnsupportedOperationException
	 */
	@Override
	public void save(OutputStream out, String comments) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException
	 */
	@Override
	public Object setProperty(String key, String value) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException
	 */
	@Override
	public void store(OutputStream out, String comments)
			throws IOException {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException
	 */
	@Override
	public void storeToXML(OutputStream os, String comment,
			String encoding) throws IOException {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException
	 */
	@Override
	public void storeToXML(OutputStream os, String comment)
			throws IOException {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException
	 */
	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException
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
	 * @throws UnsupportedOperationException
	 */
	@Override
	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException
	 */
	@Override
	public Enumeration<Object> elements() {
		//TODO
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException
	 */
	@Override
	public Set<java.util.Map.Entry<Object, Object>> entrySet() {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException
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
	 * @throws UnsupportedOperationException
	 */
	@Override
	public synchronized Enumeration<Object> keys() {
		initPropertyNames();
		return Collections.enumeration( propertyNames );
	}

	@Override
	public Set<Object> keySet() {
		initPropertyNames();
		return propertyNames;
	}

	/**
	 * @throws UnsupportedOperationException
	 */
	@Override
	public Object put(Object key, Object value) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException
	 */
	@Override
	public void putAll(Map<? extends Object, ? extends Object> t) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException
	 */
	@Override
	protected void rehash() {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException
	 */
	@Override
	public Object remove(Object key) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException
	 */
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
	 * @throws UnsupportedOperationException
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
		if ( this == obj )
			return true;
		if ( obj == null )
			return false;
		if ( getClass() != obj.getClass() )
			return false;
		final MaskedProperty other = (MaskedProperty) obj;
		if ( fallBack == null ) {
			if ( other.fallBack != null )
				return false;
		} else if ( ! fallBack.equals( other.fallBack ) )
			return false;
		if ( ! masked.equals( other.masked ) )
			return false;
		if ( ! radix.equals( other.radix ) )
			return false;
		return true;
	}
	
}
