/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.cfg;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.bridge.FieldBridge;


/**
 * As opposed to FieldBridgeMapping which is used as an option of Field,
 * this mapping can be defined directly on the property.
 * Mostly used when no Field is defined, such as on DocumentId.
 *
 * @author Sanne Grinovero (C) 2012 Red Hat Inc.
 */
public class FieldBridgeDirectMapping extends PropertyMapping {

	private final Map<String, Object> fieldBridgeAnn = new HashMap<String, Object>();

	public FieldBridgeDirectMapping(PropertyDescriptor property, EntityDescriptor entity, SearchMapping mapping, Class<? extends FieldBridge> fieldBridge) {
		super( property, entity, mapping );
		this.fieldBridgeAnn.put( "impl", fieldBridge );
		property.setFieldBridge( fieldBridgeAnn );
	}

	public FieldBridgeDirectMapping param(String name, String value) {
		Map<String, Object> param = SearchMapping.addElementToAnnotationArray( fieldBridgeAnn, "params" );
		param.put( "name", name );
		param.put( "value", value );
		return this;
	}

}
