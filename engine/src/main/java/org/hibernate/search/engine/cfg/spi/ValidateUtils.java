/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.spi;

import java.lang.invoke.MethodHandles;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.UUID;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class ValidateUtils {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private ValidateUtils() {
		// Private constructor, do not use
	}

	public static void validateCharacter(String value) {
		if ( value != null && value.length() != 1 ) {
			throw log.invalidStringForType( value, Character.class, "", null );
		}
	}

	public static void validateEnum(String value, Class<? extends Enum> enumType) {
		if ( value == null ) {
			return;
		}

		try {
			Enum.valueOf( enumType, value );
		}
		catch (IllegalArgumentException ex) {
			throw log.invalidStringForEnum( value, enumType, ex );
		}
	}

	public static void validateUUID(String value) {
		if ( value == null ) {
			return;
		}

		try {
			UUID.fromString( value );
		}
		catch (IllegalArgumentException ex) {
			throw log.invalidStringForType( value, UUID.class, ex.getMessage(), ex );
		}
	}

	public static void validateZoneId(String value) {
		if ( value == null ) {
			return;
		}

		try {
			ZoneId.of( value );
		}
		catch (DateTimeException ex) {
			throw log.invalidStringForType( value, ZoneId.class, ex.getMessage(), ex );
		}
	}

}
