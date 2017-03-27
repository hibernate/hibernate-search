/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

	private final String name;
	private final Collection<Map<String, Object>> fields = new ArrayList<Map<String, Object>>();
	private final Collection<Map<String, Object>> numericFields = new ArrayList<Map<String, Object>>();
	private final Collection<Map<String, Object>> sortableFields = new ArrayList<Map<String, Object>>();
	private final Collection<Map<String, Object>> facets = new ArrayList<Map<String, Object>>();
	private Map<String, Object> dateBridge = new HashMap<String, Object>();
	private Map<String, Object> calendarBridge = new HashMap<String, Object>();
	private Map<String,Object> indexEmbedded;
	private Map<String,Object> containedIn;

	private Map<String, Object> documentId;
	private Map<String, Object> analyzerDiscriminator;
	private Map<String, Object> dynamicBoost;
	private Map<String,Object> fieldBridge;
	private Map<String, Object> spatial;
	private Map<String, Object> latitude;
	private Map<String, Object> longitude;

	public PropertyDescriptor(String name, ElementType type) {
		this.name = name;
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

	public void addSortableField(Map<String, Object> sortableField) {
		sortableFields.add( sortableField );
	}

	public void addFacet(Map<String, Object> facet) {
		facets.add( facet );
	}

	public void setDateBridge(Map<String,Object> dateBridge) {
		this.dateBridge = dateBridge;
	}
	public void setCalendarBridge(Map<String,Object> calendarBridge) {
		this.calendarBridge = calendarBridge;
	}

	public String getName() {
		return name;
	}

	public Collection<Map<String, Object>> getFields() {
		return fields;
	}

	public Collection<Map<String, Object>> getNumericFields() {
		return numericFields;
	}

	public Collection<Map<String, Object>> getSortableFields() {
		return sortableFields;
	}

	public Collection<Map<String, Object>> getFacets() {
		return facets;
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

	public Map<String, Object> getSpatial() {
		return spatial;
	}

	public void setSpatial(Map<String, Object> spatial) {
		this.spatial = spatial;
	}

	public Map<String, Object> getLatitude() {
		return latitude;
	}

	public void setLatitude(Map<String, Object> latitude) {
		this.latitude = latitude;
	}

	public Map<String, Object> getLongitude() {
		return longitude;
	}

	public void setLongitude(Map<String, Object> longitude) {
		this.longitude = longitude;
	}
}
