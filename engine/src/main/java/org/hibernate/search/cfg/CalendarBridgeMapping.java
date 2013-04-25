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
import org.hibernate.search.SearchException;
import org.hibernate.search.annotations.Resolution;

public class CalendarBridgeMapping {

	private final SearchMapping mapping;
	private final Map<String, Object> resolution;
	private EntityDescriptor entity;
	private PropertyDescriptor property;

	public CalendarBridgeMapping(SearchMapping mapping,EntityDescriptor entity,PropertyDescriptor property, Resolution resolution) {
		if ( resolution == null ) {
			throw new SearchException( "Resolution required in order to index calendar property" );
		}
		this.mapping = mapping;
		this.resolution = new HashMap<String, Object>();
		this.entity = entity;
		this.property = property;
		this.resolution.put( "resolution", resolution );
		property.setCalendarBridge( this.resolution );
	}

	public FieldMapping field() {
		return new FieldMapping( property, entity, mapping );
	}

	public PropertyMapping property(String name, ElementType type) {
		return new PropertyMapping( name, type, entity, mapping );
	}

	public AnalyzerDefMapping analyzerDef(String name, Class<? extends TokenizerFactory> tokenizerFactory) {
		return new AnalyzerDefMapping( name, tokenizerFactory, mapping );
	}

	public EntityMapping entity(Class<?> entityType) {
		return new EntityMapping( entityType, mapping );
	}

}
