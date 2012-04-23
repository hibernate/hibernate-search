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
	private Set<Map<String, Object>> fullTextFilterDefs = new HashSet<Map<String, Object>>();
	private Map<Class<?>, EntityDescriptor> entities = new HashMap<Class<?>, EntityDescriptor>();

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

	void addFulltextFilterDef(Map<String, Object> fullTextFilterDef) {
		fullTextFilterDefs.add(fullTextFilterDef);
	}
	
}
