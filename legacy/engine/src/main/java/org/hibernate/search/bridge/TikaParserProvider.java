/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge;

import org.apache.tika.parser.Parser;

/**
 * @author Yoann Rodiere
 */
public interface TikaParserProvider {

	/**
	 * This method is called by the {@link org.hibernate.search.bridge.builtin.TikaBridge} upon initialization
	 * <p>
	 * It allows to create a custom {@link org.apache.tika.parser.Parser}.
	 *
	 * @return the Tika parser to use in the {@code TikaBridge}.
	 */
	Parser createParser();

}
