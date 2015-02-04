/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.cfg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.util.TokenizerFactory;

/**
 * Allows to configure indexing and search related aspects of a domain model using a fluent Java API. This API can be
 * used instead of or in conjunction with the annotation based configuration via
 * {@link org.hibernate.search.annotations.Indexed} etc. In case of conflicts the programmatic configuration for an
 * element takes precedence over the annotation-based configuration.
 *
 * @author Emmanuel Bernard
 */
public class SearchMapping {
	private final Set<Map<String, Object>> analyzerDefs = new HashSet<Map<String, Object>>();
	private final Set<Map<String, Object>> fullTextFilterDefs = new HashSet<Map<String, Object>>();
	private final Map<Class<?>, EntityDescriptor> entities = new HashMap<Class<?>, EntityDescriptor>();

	public Set<Map<String, Object>> getAnalyzerDefs() {
		return analyzerDefs;
	}

	public Set<Map<String, Object>> getFullTextFilterDefs() {
		return fullTextFilterDefs;
	}

	public EntityDescriptor getEntityDescriptor(Class<?> entityType) {
		return entities.get( entityType );
	}

	public Set<Class<?>> getMappedEntities() {
		return entities.keySet();
	}

	public AnalyzerDefMapping analyzerDef(String name, Class<? extends TokenizerFactory> tokenizerFactory) {
		return new AnalyzerDefMapping(name, tokenizerFactory, this);
	}

	public EntityMapping entity(Class<?> entityType) {
		return new EntityMapping(entityType, this);
	}

	public FullTextFilterDefMapping fullTextFilterDef(String name, Class<?> impl) {
		return new FullTextFilterDefMapping(this, name, impl );
	}

	/**
	 * eg @Containing(things={@Thing(...), @Thing(...)}
	 * Map<String, Object> addedThing = addElementToAnnotationArray(containing, "things");
	 */

	static Map<String, Object> addElementToAnnotationArray(Map<String, Object> containingAnnotation,
													String attributeName) {
		@SuppressWarnings("unchecked") List<Map<String, Object>> array = (List<Map<String, Object>>) containingAnnotation.get( attributeName );
		if ( array == null ) {
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
		EntityDescriptor entityDescriptor = entities.get( entityType );
		if ( entityDescriptor == null ) {
			entityDescriptor = new EntityDescriptor( );
			entities.put( entityType, entityDescriptor );
		}
		return entityDescriptor;
	}

	void addFulltextFilterDef(Map<String, Object> fullTextFilterDef) {
		fullTextFilterDefs.add( fullTextFilterDef );
	}

}
