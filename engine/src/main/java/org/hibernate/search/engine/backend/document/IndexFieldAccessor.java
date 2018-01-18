/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document;


/**
 * @author Yoann Rodiere
 */
public interface IndexFieldAccessor<T> {

	/**
	 * Add a new value to the given state for the field targeted by this reference.
	 * <p>
	 * If the field is part of nested objects, these nested objects will be created
	 * as necessary.
	 * <p>
	 * For instance, let's imagine a document with a field named {@code field} nested
	 * in two objects, resulting in the absolute path {@code parent.child.field}.
	 * If the document being produced is empty, and you call {@link #write(DocumentState, Object)}
	 * on a reference to this field, the document will look like this:
	 * <code><pre>
	 * {
	 *   "parent": {
	 *     "child": {
	 *       "field": (some value)
	 *     }
	 *   }
	 * }
	 * </pre></code>
	 * The {@code parent} and {@code child} objects have been created automatically.
	 * <p>
	 * If, instead, the document is in the following state:
	 * <code><pre>
	 * {
	 *   "parent": {
	 *     "child": {
	 *     }
	 *   }
	 * }
	 * ... then calling {@link #write(DocumentState, Object)} will result in the following document:
	 * <code><pre>
	 * {
	 *   "parent": {
	 *     "child": {
	 *       "field": (some value)
	 *     }
	 *   }
	 * }
	 * </pre></code>
	 * The existing {@code parent} and {@code child} objects have been used.
	 * <p>
	 * Finally, if the document is in the following state:
	 * <code><pre>
	 * {
	 *   "parent": {
	 *     "child": null
	 *   }
	 * }
	 * </pre></code>
	 * ... then calling {@link #write(DocumentState, Object)} will result in the following document:
	 * <code><pre>
	 * {
	 *   "parent": {
	 *     "child": [
	 *       null,
	 *       {
	 *         "field": (some value)
	 *       }
	 *     ]
	 *   }
	 * }
	 * </pre></code>
	 *
	 * @param target
	 * @param value
	 */
	void write(DocumentState target, T value);

}
