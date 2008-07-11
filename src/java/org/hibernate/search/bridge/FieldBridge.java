//$Id$
package org.hibernate.search.bridge;

import org.apache.lucene.document.Document;

/**
 * Link between a java property and a Lucene Document
 * Usually a Java property will be linked to a Document Field
 *
 * @author Emmanuel Bernard
 */
public interface FieldBridge {

	/**
	 * Manipulate the document to index the given value.
	 * A common implementation is to add a Field <code>name</code> to the given document following
	 * the parameters (<code>store</code>, <code>index</code>, <code>boost</code>) if the
	 * <code>value</code> is not null
	 * @param luceneOptions Contains the parameters used for adding <code>value</code> to 
	 * the Lucene <code>document</code>.
	 */
	void set(String name, Object value, Document document, LuceneOptions luceneOptions);
}
