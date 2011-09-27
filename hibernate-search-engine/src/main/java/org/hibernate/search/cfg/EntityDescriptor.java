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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Emmanuel Bernard
 */
public class EntityDescriptor {
	private Class<?> entityType;
	private Map<String, Object> indexed;
	private Map<PropertyKey, PropertyDescriptor> properties = new HashMap<PropertyKey, PropertyDescriptor>();
	private Map<String, Object> similarity;
	private Map<String, Object> boost;
	private Map<String, Object> analyzerDiscriminator;
	private Set<Map<String, Object>> fullTextFilterDefs = new HashSet<Map<String, Object>>();
	private Map<String,Object> providedId;
	private Set<Map<String,Object>> classBridges = new HashSet<Map<String,Object>>();
	private Map<String, Object> dynamicBoost;
	private Map<String, Object> cacheInMemory;
	
	public Map<String, Object> getIndexed() {
		return indexed;
	}

	public EntityDescriptor(Class<?> entityType) {
		this.entityType = entityType;
	}

	public void setIndexed(Map<String, Object> indexed) {
		this.indexed = indexed;
	}

	PropertyDescriptor getProperty(String name, ElementType type) {
		PropertyKey propertyKey = new PropertyKey( name, type );
		PropertyDescriptor descriptor = properties.get( propertyKey );
		if (descriptor == null) {
			descriptor = new PropertyDescriptor(name, type);
			properties.put(propertyKey, descriptor);
		}
		return descriptor;
	}

	public PropertyDescriptor getPropertyDescriptor(String name, ElementType type) {
		return properties.get( new PropertyKey( name, type ) );
	}

	public void setSimilariy(Map<String, Object> similarity) {
		this.similarity = similarity;
	}
	
	public Map<String, Object> getSimilarity() {
		return similarity;
	}
	
	public Map<String, Object> getCacheInMemory() {
		return cacheInMemory;
	}

	public void setCacheInMemory(Map<String, Object> cacheInMemory) {
		this.cacheInMemory = cacheInMemory;
	}

	public void setBoost(Map<String, Object> boost) {
		this.boost = boost;
	}

	public Map<String, Object> getBoost() {
		return boost;
	}

	public void setAnalyzerDiscriminator(Map<String, Object> analyzerDiscriminator) {
		this.analyzerDiscriminator = analyzerDiscriminator;
	}

	public Map<String, Object> getAnalyzerDiscriminator() {
		return analyzerDiscriminator;
	}
	
	public Set<Map<String, Object>> getFullTextFilterDefs() {
		return fullTextFilterDefs;
	}
	
	public void addFulltextFilterDef(Map<String, Object> fullTextFilterDef) {
		fullTextFilterDefs.add(fullTextFilterDef);
	}

	
	public void addClassBridgeDef(Map<String,Object> classBridge) {
		classBridges.add(classBridge);
	}
	
	public Set<Map<String, Object>> getClassBridgeDefs() {
		return classBridges;
	}
	
	public void setProvidedId(Map<String, Object> providedId) {
		this.providedId = providedId;
	}
	
	public Map<String, Object> getProvidedId() {
		return this.providedId;
	}
	
	public void setDynamicBoost(Map<String, Object> dynamicEntityBoost) {
		this.dynamicBoost = dynamicEntityBoost;
	}
	
	public Map<String, Object> getDynamicBoost() {
		return this.dynamicBoost;
	}
	
	private static class PropertyKey {
		private String name;
		private ElementType type;

		PropertyKey(String name, ElementType type) {
			this.name = name;
			this.type = type;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			PropertyKey property = ( PropertyKey ) o;

			if ( name != null ? !name.equals( property.name ) : property.name != null ) {
				return false;
			}
			if ( type != property.type ) {
				return false;
			}

			return true;
		}

		@Override
		public int hashCode() {
			int result = name != null ? name.hashCode() : 0;
			result = 31 * result + ( type != null ? type.hashCode() : 0 );
			return result;
		}
	}

}
