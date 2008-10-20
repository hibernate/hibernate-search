//$Id$
package org.hibernate.search.bridge;

import org.apache.lucene.document.Document;

/**
 * A FieldBrige able to convert the index representation back into an object without losing information
 *
 * Any bridge expected to process a document id should implement this interface
 * EXPERIMENTAL Consider this interface as private
 *
 * @author Emmanuel Bernard
 */
//FIXME rework the interface inheritance there are some common concepts with StringBridge
public interface TwoWayFieldBridge extends FieldBridge {
	/**
	 * build the element object from the Document
	 *
	 * The return value is the Entity id
	 *
	 * @param name	 field name
	 * @param document document
	 */
	Object get(String name, Document document);

	/**
	 * convert the object representation to a String
	 * The return String must not be null, it can be empty though
	 */
	String objectToString(Object object);
}
