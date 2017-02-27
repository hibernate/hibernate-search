/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.cfg.spi;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Helper to convert the org.hibernate.search.annotations.Parameter
 * annotations.
 */
public final class ParameterAnnotationsReader {

	private static final Log log = LoggerFactory.make();

	private ParameterAnnotationsReader() {
		// Not to be constructed
	}

	/**
	 * Converts the Parameter key/value pairs in a map, and validates
	 * against conflicting duplicates.
	 * Any duplicate will cause to throw a SearchException
	 * @param parameters
	 * @return a new Map instance containing the key/value pairs
	 * @throws org.hibernate.search.exception.SearchException
	 */
	public static Map<String, String> toNewMutableMap(Parameter[] parameters) {
		Map<String, String> map = new LinkedHashMap<>();
		if ( parameters != null ) {
			for ( Parameter param : parameters ) {
				String previous = map.put( param.name(), param.value() );
				if ( previous != null ) {
					throw log.conflictingParameterDefined( param.name(), param.value(), previous );
				}
			}
		}
		return map;
	}

}
