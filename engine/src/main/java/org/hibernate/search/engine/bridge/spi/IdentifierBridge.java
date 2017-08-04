/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.bridge.spi;


/**
 * Converts an identifier to a unique String representation and back.
 *
 * @author Yoann Rodiere
 */
public interface IdentifierBridge<T> extends AutoCloseable {

	String toString(T id);

	T fromString(String idString);

	@Override
	default void close() {
	}

}
