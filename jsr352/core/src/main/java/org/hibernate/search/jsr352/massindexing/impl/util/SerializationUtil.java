/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.impl.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

import org.hibernate.search.exception.SearchException;
import org.hibernate.search.jsr352.logging.impl.Log;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Mincong Huang
 */
public final class SerializationUtil {

	private static final Log log = LoggerFactory.make( Log.class );

	private SerializationUtil() {
		// Private constructor, do not use it.
	}

	public static String serialize(Object object) throws IOException {
		try ( ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream( baos ) ) {
			oos.writeObject( object );
			oos.flush();
			byte bytes[] = baos.toByteArray();
			return Base64.getEncoder().encodeToString( bytes );
		}
	}

	public static Object deserialize(String serialized) throws IOException, ClassNotFoundException {
		if ( StringHelper.isEmpty( serialized ) ) {
			return null;
		}
		byte bytes[] = Base64.getDecoder().decode( serialized );
		try ( ByteArrayInputStream bais = new ByteArrayInputStream( bytes );
				ObjectInputStream ois = new ObjectInputStream( bais ) ) {
			return ois.readObject();
		}
	}

	/**
	 * Given a parameter key-value pair, parses the value into boolean.
	 * <p>
	 * Only string literals "true" and "false" are allowed, where case is
	 * ignored. Parsing any other value, such as {@code null}, "", "t", "f",
	 * "0", "1", will lead to a parsing failure.
	 *
	 * @throws SearchException if the parsing fails.
	 */
	public static boolean parseBooleanParameter(String key, String value) {
		if ( "true".equalsIgnoreCase( value ) ) {
			return true;
		}
		if ( "false".equalsIgnoreCase( value ) ) {
			return false;
		}
		throw log.unableToParseJobParameter( key, value, null );
	}

	public static int parseIntegerParameter(String key, String value) {
		try {
			return Integer.parseInt( value );
		}
		catch (NumberFormatException e) {
			throw log.unableToParseJobParameter( key, value, e );
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T parseParameter(@SuppressWarnings("unused") Class<T> type, String key, String value) {
		try {
			return (T) SerializationUtil.deserialize( value );
		}
		catch (IOException | ClassNotFoundException e) {
			throw log.unableToParseJobParameter( key, value, e );
		}
	}

}
