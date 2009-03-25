package org.hibernate.search.cfg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.solr.analysis.TokenizerFactory;

/**
 * @author Emmanuel Bernard
 */
public class SearchMapping {
	private Set<Map<String, Object>> analyzerDefs = new HashSet<Map<String, Object>>();
	private Map<Class<?>, EntityDescriptor> entities = new HashMap<Class<?>, EntityDescriptor>();

	public Set<Map<String, Object>> getAnalyzerDefs() {
		return analyzerDefs;
	}

	public EntityDescriptor getEntityDescriptor(Class<?> entityType) {
		return entities.get( entityType );
	}

	public AnalyzerDefMapping analyzerDef(String name, Class<? extends TokenizerFactory> tokenizerFactory) {
		return new AnalyzerDefMapping(name, tokenizerFactory, this);
	}

	public EntityMapping indexedClass(Class<?> entityType) {
		return new EntityMapping(entityType, null, this);
	}

	public EntityMapping indexedClass(Class<?> entityType, String indexName) {
		return new EntityMapping(entityType, indexName,  this);
	}

	/**
	 * eg @Containing(things={@Thing(...), @Thing(...)}
	 * Map<String, Object> addedThing = addElementToAnnotationArray(containing, "things");
	 */
	static Map<String, Object> addElementToAnnotationArray(Map<String, Object> containingAnnotation,
													  String attributeName) {
		List<Map<String, Object>> array = (List<Map<String, Object>>) containingAnnotation.get( attributeName );
		if ( array == null) {
			array = new ArrayList<Map<String, Object>>();
			containingAnnotation.put( attributeName, array );
		}
		Map<String, Object> param = new HashMap<String, Object>();
		array.add( param );
		return param;
	}

	void addAnalyzerDef(Map<String, Object> analyzerDef) {
		analyzerDefs.add( analyzerDef );
	}

	EntityDescriptor getEntity(Class<?> entityType) {
		EntityDescriptor entity = entities.get( entityType );
		if (entity == null) {
			entity = new EntityDescriptor(entityType);
			entities.put( entityType, entity );
		}
		return entity;
	}
}
