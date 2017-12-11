/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.spi;


/**
 * @author Yoann Rodiere
 */
public interface IndexObjectReference {

	/**
	 * Add a new object to the given state at the target of this reference.
	 * <p>
	 * This method is only useful when you want to write more than one object
	 * to a given path.
	 * <p>
	 * For instance, let's imagine a document with a field named {@code field} nested
	 * in two objects, resulting in the absolute path {@code parent.child.field}.
	 * If the document being produced is empty, and you call {@link IndexFieldAccessor#write(DocumentState, Object)}
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
	 * Then if you call it a second time, the document will look like this:
	 * <code><pre>
	 * {
	 *   "parent": {
	 *     "child": {
	 *       "field": [(some value), (some other value)]
	 *     }
	 *   }
	 * }
	 * </pre></code>
	 * Thus you have one parent, one child, and two fields.
	 *
	 * <p>If, instead of the result above, you would like one parent, two childs with each one field,
	 * you will call {@link IndexFieldAccessor#write(DocumentState, Object)}, then {@link #add(DocumentState)}
	 * on a reference to the child object, then {@link IndexFieldAccessor#write(DocumentState, Object)}
	 * again. The document will then look like this:
	 * <code><pre>
	 * {
	 *   "parent": {
	 *     "child": [{
	 *       "field": (some value)
	 *     }, {
	 *       "field": (some other value)
	 *     }]
	 *   }
	 * }
	 * </pre></code>
	 *
	 * @param target
	 * @return
	 */
	void add(DocumentState target);


	/**
	 * Similar to {@link #add(DocumentState)}, but instead of adding an
	 * object, this method adds a missing value marker.
	 * <p>
	 * The missing value marker may have a representation in the backend (such as a JSON {@code null}),
	 * or it may be ignored completely, depending on the implementation.
	 *
	 * @param target
	 * @return
	 */
	void addMissing(DocumentState target);

}
