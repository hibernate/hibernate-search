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
import java.util.Map;

import org.apache.solr.analysis.TokenizerFactory;

public class IndexEmbeddedMapping {

	private final SearchMapping mapping;
	private final Map<String,Object> indexEmbedded;
	private EntityDescriptor entity;
	private PropertyDescriptor property;
	
	public IndexEmbeddedMapping(SearchMapping mapping, PropertyDescriptor property, EntityDescriptor entity) {
		this.mapping = mapping;
		this.indexEmbedded = new HashMap<String, Object>();
		this.property = property;
		this.entity = entity;
		this.property.setIndexEmbedded(indexEmbedded);
	}
	
	public IndexEmbeddedMapping prefix(String prefix) {
		this.indexEmbedded.put("prefix",prefix);
		return this;
	}
	
	public IndexEmbeddedMapping targetElement(Class<?> targetElement) {
		this.indexEmbedded.put("targetElement",targetElement);
		return this;
	}
	
	public IndexEmbeddedMapping depth(int depth) {
		this.indexEmbedded.put("depth", depth);
		return this;
	}
	
	public PropertyMapping property(String name, ElementType type) {
		return new PropertyMapping(name, type, entity, mapping);
	}

	public AnalyzerDefMapping analyzerDef(String name, Class<? extends TokenizerFactory> tokenizerFactory) {
		return new AnalyzerDefMapping(name, tokenizerFactory, mapping);
	}

	public EntityMapping entity(Class<?> entityType) {
		return new EntityMapping(entityType, mapping);
	}
	
	public FieldMapping field() {
		return new FieldMapping(property, entity, mapping);
	}

}
