// $Id$
package org.hibernate.search.backend.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Collection;
import java.util.Enumeration;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper to Properties, to restrict the availability of
 * values to only those which have a key beginning with some
 * masking String.
 * Methods to list available keys or values or otherwise
 * enumerate available properties are not available.
 * 
 * @author Sanne Grinovero
 */
public class MaskedProperty extends Properties implements Serializable {
	
	private static final long serialVersionUID = -593307257383085113L;
	
	private transient Logger log = LoggerFactory.getLogger( MaskedProperty.class );
	private final Properties masked;
	private final Properties fallBack;
	private final String radix;
	
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
			log.trace( "found a match for key: [{}] value: {}", compositeKey, value );
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
	public boolean containsKey(Object key) {
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

	/**
	 * @throws UnsupportedOperationException
	 */
	@Override
	public Enumeration<?> propertyNames() {
		throw new UnsupportedOperationException();
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

	/**
	 * @throws UnsupportedOperationException
	 */
	@Override
	public boolean contains(Object value) {
		throw new UnsupportedOperationException();
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
	public boolean isEmpty() {
		if ( fallBack==null ) {
			return masked.isEmpty();
		}
		else {
			return masked.isEmpty() && fallBack.isEmpty();
		}
	}

	/**
	 * @throws UnsupportedOperationException
	 */
	@Override
	public Enumeration<Object> keys() {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException
	 */
	@Override
	public Set<Object> keySet() {
		throw new UnsupportedOperationException();
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
	public int size() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return masked.toString();
	}

	/**
	 * @throws UnsupportedOperationException
	 */
	@Override
	public Collection<Object> values() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = ( ( fallBack == null ) ? 0 : fallBack.hashCode() );
		result = prime * result + masked.hashCode();
		result = prime * result + radix.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
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
	
	private void readObject(ObjectInputStream aInputStream) throws ClassNotFoundException, IOException {
		//always perform the default de-serialization first
		aInputStream.defaultReadObject();
		log = LoggerFactory.getLogger( MaskedProperty.class );
	}

	private void writeObject(ObjectOutputStream aOutputStream) throws IOException {
		aOutputStream.defaultWriteObject();
	}
	
}
