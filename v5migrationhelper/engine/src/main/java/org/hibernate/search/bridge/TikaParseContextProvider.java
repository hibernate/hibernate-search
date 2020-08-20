/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge;

import org.apache.tika.parser.ParseContext;

/**
 * @author Hardy Ferentschik
 */
public interface TikaParseContextProvider {

	/**
	 * This method is called by the Tika bridge prior to parsing the data.
	 * <p>
	 * It allows to create a custom {@code ParseContext}
	 *
	 * @param name the field name of the property which is processed by the Tika bridge
	 * @param value the value to be indexed
	 * @return A {@code ParseContext} instance used by the Tika bridge to parse the data
	 * @see <a href="http://tika.apache.org/1.1/parser.html#apiorgapachetikametadataMetadata.html">Tika API</a>
	 */
	ParseContext getParseContext(String name, Object value);

}
