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
@SuppressWarnings("unchecked")
public class EquipmentType implements FieldBridge, ParameterizedBridge {
	private Map equips;

	public void setParameterValues(Map parameters) {
		// This map was defined by the parameters of the ClassBridge annotation.
		this.equips = parameters;
	}

	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		// In this particular class the name of the new field was passed
		// from the name field of the ClassBridge Annotation. This is not
		// a requirement. It just works that way in this instance. The
		// actual name could be supplied by hard coding it below.
		Departments deps = (Departments) value;
		Field field = null;
		String fieldValue1 = deps.getManufacturer();

		if ( fieldValue1 == null ) {
			fieldValue1 = "";
		}
		else {
			String fieldValue = (String) equips.get( fieldValue1 );
			field = new Field( name, fieldValue, luceneOptions.getStore(), luceneOptions.getIndex(), luceneOptions.getTermVector() );
			field.setBoost( luceneOptions.getBoost() );
		}
		document.add( field );
	}
}
