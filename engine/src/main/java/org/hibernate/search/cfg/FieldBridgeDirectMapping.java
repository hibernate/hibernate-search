/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
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
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
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
		param.put("name", name);
		param.put("value", value);
		return this;
	}

}
