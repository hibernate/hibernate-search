//$Id$
package org.hibernate.search.bridge;

import org.apache.lucene.document.Document;

/**
 * A <code>FieldBridge</code> able to convert the index representation back into an object without losing information.
 * Any bridge expected to process a document id should implement this interface.
 *
 * @author Emmanuel Bernard
 */
// FIXME rework the interface inheritance there are some common concepts with StringBridge
public interface TwoWayFieldBridge extends FieldBridge {
	/**
	 * Build the element object from the <code>Document</code>
	 *
	 * @param name field name
	 * @param document document
	 *
	 * @return The return value is the entity property value.
	 */
	Object get(String name, Document document);

	/**
	 * Convert the object representation to a string.
	 *
	 * @param object The object to index.
	 * @return string (index) representationT of the specified object. Must not be <code>null</code>, but
	 *         can be empty.
	 */
	String objectToString(Object object);
}
