/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge;

import java.util.Map;

import org.apache.lucene.document.Document;

import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.ParameterizedBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;

/**
 * @author John Griffin
 */
public class EquipmentType implements TwoWayFieldBridge, ParameterizedBridge {

	private Map<String,String> equips;

	@Override
	public void setParameterValues(Map parameters) {
		// This map was defined by the parameters of the ClassBridge annotation.
		this.equips = parameters;
	}

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		// In this particular class the name of the new field was passed
		// from the name field of the ClassBridge Annotation. This is not
		// a requirement. It just works that way in this instance. The
		// actual name could be supplied by hard coding it below.

		String indexedString = objectToString( value );
		if ( !indexedString.isEmpty() ) {
			luceneOptions.addFieldToDocument( name, indexedString, document );
		}
	}

	@Override
	public Object get(String name, Document document) {
		return document.get( name );
	}

	@Override
	public String objectToString(Object value) {
		Departments deps = (Departments) value;
		String fieldValue1 = deps.getManufacturer();
		String result = null;

		if ( fieldValue1 != null ) {
			result = equips.get( fieldValue1 );
		}
		if ( result == null ) {
			result = "";
		}

		return result;
	}
}
