/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jakarta.batch.core.massindexing.util.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.invoke.MethodHandles;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;

import org.hibernate.CacheMode;
import org.hibernate.search.jakarta.batch.core.logging.impl.Log;
import org.hibernate.search.mapper.orm.loading.spi.ConditionalExpression;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.StringHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * @author Mincong Huang
 */
public final class SerializationUtil {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

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
	public static boolean parseBooleanParameterOptional(String key, String value, boolean defaultValue) {
		if ( value == null ) {
			return defaultValue;
		}
		if ( "true".equalsIgnoreCase( value ) ) {
			return true;
		}
		if ( "false".equalsIgnoreCase( value ) ) {
			return false;
		}
		throw log.unableToParseJobParameter( key, value, "", null );
	}

	public static int parseIntegerParameter(String key, String value) {
		try {
			return Integer.parseInt( value );
		}
		catch (NumberFormatException e) {
			throw log.unableToParseJobParameter( key, value, e.getMessage(), e );
		}
	}

	public static Integer parseIntegerParameterOptional(String key, String value, Integer defaultValue) {
		if ( value == null ) {
			return defaultValue;
		}
		else {
			return parseIntegerParameter( key, value );
		}
	}

	public static CacheMode parseCacheModeParameter(String key, String value, CacheMode defaultValue) {
		return parseEnumParameter( CacheMode.class, key, value == null ? value : value.toUpperCase( Locale.ROOT ),
				defaultValue );
	}

	private static <T extends Enum<T>> T parseEnumParameter(Class<T> clazz, String key, String value, T defaultValue) {
		if ( value == null ) {
			return defaultValue;
		}
		try {
			return Enum.valueOf( clazz, value );
		}
		catch (IllegalArgumentException e) {
			throw log.unableToParseJobParameter( key, value, e.getMessage(), e );
		}
	}

	public static ConditionalExpression parseReindexOnlyParameters(String reindexOnlyHql,
			String serializedReindexOnlyParameters)
			throws IOException, ClassNotFoundException {
		if ( reindexOnlyHql == null ) {
			return null;
		}
		else {
			ConditionalExpression reindexOnly = new ConditionalExpression( reindexOnlyHql );
			@SuppressWarnings("unchecked")
			Map<String, ?> params = (Map<String, ?>) SerializationUtil.deserialize( serializedReindexOnlyParameters );
			if ( params != null ) {
				params.forEach( reindexOnly::param );
			}
			return reindexOnly;
		}

	}
}
