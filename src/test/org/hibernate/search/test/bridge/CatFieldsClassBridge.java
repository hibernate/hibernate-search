// $Id$
package org.hibernate.search.test.bridge;

import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.ParameterizedBridge;

/**
 * @author John Griffin
 */
public class CatFieldsClassBridge implements FieldBridge, ParameterizedBridge {

	private String sepChar;

	@SuppressWarnings("unchecked")
	public void setParameterValues(Map parameters) {
		this.sepChar = (String) parameters.get( "sepChar" );
	}

	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		// In this particular class the name of the new field was passed
		// from the name field of the ClassBridge Annotation. This is not
		// a requirement. It just works that way in this instance. The
		// actual name could be supplied by hard coding it below.
		Department dep = (Department) value;
		String fieldValue1 = dep.getBranch();
		if ( fieldValue1 == null ) {
			fieldValue1 = "";
		}
		String fieldValue2 = dep.getNetwork();
		if ( fieldValue2 == null ) {
			fieldValue2 = "";
		}
		String fieldValue = fieldValue1 + sepChar + fieldValue2;
		Field field = new Field( name, fieldValue, luceneOptions.getStore(), luceneOptions.getIndex(), luceneOptions.getTermVector() );
		field.setBoost( luceneOptions.getBoost() );
		document.add( field );
	}
}
