/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge;

/**
 * Transform an object into a string representation.
 *
 * All implementations are required to be threadsafe.
 * Usually this is easily achieved avoiding the usage
 * of class fields, unless they are either immutable
 * or needed to store parameters.
 *
 * @author Emmanuel Bernard
 */
public interface StringBridge {

	/**
	 * Converts the object representation to a string.
	 *
	 * @param object The object to transform into a string representation.
	 * @return String representation of the given object to be stored in Lucene index. The return string must not be
	 * <code>null</code>. It can be empty though.
	 */
	String objectToString(Object object);
}
