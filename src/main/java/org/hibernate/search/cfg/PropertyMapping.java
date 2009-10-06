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

import org.apache.solr.analysis.TokenizerFactory;

/**
 * @author Emmanuel Bernard
 */
public class PropertyMapping {
	private SearchMapping mapping;
	private EntityDescriptor entity;
	private PropertyDescriptor property;

	public PropertyMapping(String name, ElementType type, EntityDescriptor entity, SearchMapping mapping) {
		this.mapping = mapping;
		this.entity = entity;
		property = entity.getProperty(name, type);
	}

	public DocumentIdMapping documentId() {
		return new DocumentIdMapping( property, entity, mapping );
	}

	public FieldMapping field() {
		return new FieldMapping(property, entity, mapping);
	}

	public PropertyMapping property(String name, ElementType type) {
		return new PropertyMapping(name, type, entity, mapping);
	}

	public AnalyzerDefMapping analyzerDef(String name, Class<? extends TokenizerFactory> tokenizerFactory) {
		return new AnalyzerDefMapping(name, tokenizerFactory, mapping);
	}

	public EntityMapping indexedClass(Class<?> entityType) {
		return new EntityMapping(entityType, null, mapping);
	}

	public EntityMapping indexedClass(Class<?> entityType, String indexName) {
		return new EntityMapping(entityType, indexName,  mapping);
	}
}
