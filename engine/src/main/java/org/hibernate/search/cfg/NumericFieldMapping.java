/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.cfg;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Gustavo Fernandes
 */
public class NumericFieldMapping extends FieldMapping {

	private final Map<String, Object> numericField = new HashMap<String, Object>( 2 );

	/**
	 * @deprecated Use {@link #NumericFieldMapping(String, PropertyDescriptor, EntityDescriptor, SearchMapping)} instead.
	 */
	@Deprecated
	public NumericFieldMapping(PropertyDescriptor property, EntityDescriptor entity, SearchMapping mapping) {
		this( property.getName(), property, entity, mapping );
	}

	public NumericFieldMapping(String fieldName, PropertyDescriptor property, EntityDescriptor entity, SearchMapping mapping) {
		super( property, entity, mapping );
		numericField.put( "forField", fieldName );
		property.addNumericField( numericField );
	}

	public NumericFieldMapping precisionStep(int precisionStep) {
		numericField.put( "precisionStep", precisionStep );
		return this;
	}
}
