/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration;

import java.util.Map;

import org.apache.lucene.document.Document;

import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.ParameterizedBridge;

/**
 * @author John Griffin
 */
public class EquipmentType implements FieldBridge, ParameterizedBridge {

	private Map<String,String> equips;

	@Override
	public void setParameterValues(Map<String,String> parameters) {
		// This map was defined by the parameters of the ClassBridge annotation.
		this.equips = parameters;
	}

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		// In this particular class the name of the new field was passed
		// from the name field of the ClassBridge Annotation. This is not
		// a requirement. It just works that way in this instance. The
		// actual name could be supplied by hard coding it below.
		Departments deps = (Departments) value;
		String fieldValue = deps.getManufacturer();

		if ( fieldValue != null ) {
			String indexedString = equips.get( fieldValue );
			luceneOptions.addFieldToDocument( name, indexedString, document );
		}
	}

}
