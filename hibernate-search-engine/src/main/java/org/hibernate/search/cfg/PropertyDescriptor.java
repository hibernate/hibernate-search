/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Emmanuel Bernard
 */
public class PropertyDescriptor {
	private ElementType type;
	private String name;
	private Collection<Map<String, Object>> fields = new ArrayList<Map<String, Object>>();
	private Collection<Map<String, Object>> numericFields = new ArrayList<Map<String, Object>>();
	private Map<String, Object> dateBridge= new HashMap<String, Object>();
	private Map<String, Object> calendarBridge= new HashMap<String, Object>();
	private Map<String,Object> indexEmbedded;
	private Map<String,Object> containedIn;

	private Map<String, Object> documentId;
	private Map<String, Object> analyzerDiscriminator;
	private Map<String, Object> dynamicBoost;
	private Map<String,Object> fieldBridge;

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

	public void addNumericField(Map<String, Object> numericField) {
		numericFields.add( numericField );
	}

	public void setDateBridge(Map<String,Object> dateBridge) {
		this.dateBridge = dateBridge;
	}
	public void setCalendarBridge(Map<String,Object> calendarBridge) {
		this.calendarBridge = calendarBridge;
	}
	
	public Collection<Map<String, Object>> getFields() {
		return fields;
	}

	public Collection<Map<String, Object>> getNumericFields() {
		return numericFields;
	}

	public Map<String, Object> getDocumentId() {
		return documentId;
	}

	public Map<String, Object> getAnalyzerDiscriminator() {
		return analyzerDiscriminator;
	}

	
	public Map<String, Object> getDateBridge() {
		return dateBridge;
	}
	public Map<String, Object> getCalendarBridge() {
		return calendarBridge;
	}
	
	
	public void setAnalyzerDiscriminator(Map<String, Object> analyzerDiscriminator) {
		this.analyzerDiscriminator = analyzerDiscriminator;
	}
	
	public Map<String, Object> getIndexEmbedded() {
		return indexEmbedded;
	}

	public void setIndexEmbedded(Map<String, Object> indexEmbedded) {
		this.indexEmbedded = indexEmbedded;
	}
	public Map<String, Object> getContainedIn() {
		return containedIn;
	}

	public void setContainedIn(Map<String, Object> containedIn) {
		this.containedIn = containedIn;
	}

	public void setDynamicBoost(Map<String, Object> dynamicBoostAnn) {
		this.dynamicBoost = dynamicBoostAnn;
	}
	
	public Map<String,Object> getDynamicBoost() {
		return this.dynamicBoost;
	}

	public Map<String, Object> getFieldBridge() {
		return fieldBridge;
	}

	public void setFieldBridge(Map<String, Object> fieldBridge) {
		this.fieldBridge = fieldBridge;
	}

}
