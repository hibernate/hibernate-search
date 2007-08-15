//$Id$
package org.hibernate.search.bridge;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

/**
 * Link between a java property and a Lucene Document
 * Usually a Java property will be linked to a Document Field
 *
 * @author Emmanuel Bernard
 */
//TODO should show Field or document?
//document is nice since I can save an object into several fields
public interface FieldBridge {
	/**
	 * Manipulate the document to index the given value.
	 * A common implementation is to add a Field <code>name</code> to the given document following
	 * the parameters (<code>store</code>, <code>index</code>, <code>boost</code>) if the
	 * <code>value</code> is not null
	 */
	void set(String name, Object value, Document document, Field.Store store, Field.Index index, Float boost);
}
