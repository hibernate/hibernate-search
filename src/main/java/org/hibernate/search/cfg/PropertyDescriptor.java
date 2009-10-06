/* $Id$
 * 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 * 
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.cfg;

import java.lang.annotation.ElementType;
import java.util.Map;
import java.util.Collection;
import java.util.ArrayList;

/**
 * @author Emmanuel Bernard
 */
public class PropertyDescriptor {
	private ElementType type;
	private String name;
	private Collection<Map<String, Object>> fields = new ArrayList<Map<String, Object>>();
	private Map<String, Object> documentId;

	public PropertyDescriptor(String name, ElementType type) {
		this.name = name;
		this.type = type;
	}
	
	public void setDocumentId(Map<String, Object> documentId) {
		this.documentId = documentId;
	}

	public void addField(Map<String, Object> field) {
		fields.add( field );
	}

	public Collection<Map<String, Object>> getFields() {
		return fields;
	}

	public Map<String, Object> getDocumentId() {
		return documentId;
	}
}
