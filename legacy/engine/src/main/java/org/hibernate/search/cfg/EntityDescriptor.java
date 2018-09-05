/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.cfg;

import java.lang.annotation.ElementType;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.exception.SearchException;
import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.bridge.FieldBridge;

/**
 * @author Emmanuel Bernard
 */
public class EntityDescriptor {
	private Map<String, Object> indexed;
	private final Map<PropertyKey, PropertyDescriptor> properties = new HashMap<PropertyKey, PropertyDescriptor>();
	private Map<String, Object> boost;
	private Map<String, Object> analyzerDiscriminator;
	private final Set<Map<String, Object>> fullTextFilterDefs = new HashSet<Map<String, Object>>();
	private Map<String, Object> providedId;

	/**
	 * Configured class bridges. Each bridge is represented by a map with {@code @ClassBridge} annotation member values
	 * keyed by annotation member name.
	 */
	private final Set<Map<String, Object>> classBridges = new HashSet<Map<String, Object>>();

	/**
	 * Class bridge instances and their configuration
	 */
	private final Map<FieldBridge, Map<String, Object>> classBridgeInstanceDefs = new IdentityHashMap<FieldBridge, Map<String,Object>>();

	/**
	 * Class bridge instances and their configuration in form of a {@code ClassBridge} annotation
	 */
	private final Map<FieldBridge, ClassBridge> classBridgeConfigurations = new IdentityHashMap<FieldBridge, ClassBridge>();
	private final Set<Map<String, Object>> spatials = new HashSet<Map<String, Object>>();
	private Map<String, Object> dynamicBoost;

	public Map<String, Object> getIndexed() {
		return indexed;
	}

	public void setIndexed(Map<String, Object> indexed) {
		this.indexed = indexed;
	}

	PropertyDescriptor getProperty(String name, ElementType type) {
		PropertyKey propertyKey = new PropertyKey( name, type );
		PropertyDescriptor descriptor = properties.get( propertyKey );
		if ( descriptor == null ) {
			descriptor = new PropertyDescriptor( name, type );
			properties.put( propertyKey, descriptor );
		}
		return descriptor;
	}

	public PropertyDescriptor getPropertyDescriptor(String name, ElementType type) {
		return properties.get( new PropertyKey( name, type ) );
	}

	/**
	 * This feature will be removed, with no replacement
	 * as caching fields is no longer effective.
	 * @return This will always return an empty Map.
	 * @deprecated This will be removed with no replacement.
	 */
	@Deprecated
	public Map<String, Object> getCacheInMemory() {
		return Collections.<String, Object>emptyMap();
	}

	/**
	 * This feature will be removed, with no replacement
	 * as caching fields is no longer effective.
	 * @param cacheInMemory this parameter will be ignored.
	 * @deprecated This will be removed with no replacement.
	 */
	@Deprecated
	public void setCacheInMemory(Map<String, Object> cacheInMemory) {
		//No-op: this feature is no longer available
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
		fullTextFilterDefs.add( fullTextFilterDef );
	}

	public void addClassBridgeDef(Map<String, Object> classBridge) {
		classBridges.add( classBridge );
	}

	public void addClassBridgeInstanceDef(FieldBridge classBridge, Map<String, Object> properties) {
		Map<String, Object> previous = classBridgeInstanceDefs.put( classBridge, properties );

		if ( previous != null ) {
			throw new SearchException( "The same field bridge instance must not be passed more than once." );
		}
	}

	public Set<Map<String, Object>> getClassBridgeDefs() {
		return classBridges;
	}

	public Map<FieldBridge, Map<String, Object>> getClassBridgeInstanceDefs() {
		return classBridgeInstanceDefs;
	}

	public void addClassBridgeInstanceConfiguration(FieldBridge classBridge, ClassBridge configuration) {
		classBridgeConfigurations.put( classBridge, configuration );
	}

	public Map<FieldBridge, ClassBridge> getClassBridgeConfigurations() {
		return classBridgeConfigurations;
	}

	public void addSpatial(Map<String,Object> spatial) {
		spatials.add( spatial );
	}

	public Set<Map<String, Object>> getSpatials() {
		return spatials;
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
		private final String name;
		private final ElementType type;

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

			PropertyKey property = (PropertyKey) o;

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
