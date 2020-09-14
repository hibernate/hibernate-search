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
public class CatDeptsFieldsClassBridge implements TwoWayFieldBridge, ParameterizedBridge {

	private String sepChar;

	@Override
	public void setParameterValues(Map<String,String> parameters) {
		this.sepChar = parameters.get( "sepChar" );
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
		Departments dep = (Departments) value;
		String fieldValue1 = dep.getBranch();
		if ( fieldValue1 == null ) {
			fieldValue1 = "";
		}
		String fieldValue2 = dep.getNetwork();
		if ( fieldValue2 == null ) {
			fieldValue2 = "";
		}
		return fieldValue1 + sepChar + fieldValue2;
	}
}
