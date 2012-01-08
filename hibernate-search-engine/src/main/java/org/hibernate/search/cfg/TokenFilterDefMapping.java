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

import java.util.Map;

import org.apache.solr.analysis.TokenFilterFactory;
import org.apache.solr.analysis.TokenizerFactory;

/**
 * @author Emmanuel Bernard
 */
public class TokenFilterDefMapping {
	private Map<String, Object> filter;
	private Map<String, Object> analyzerDef;
	private SearchMapping mapping;

	TokenFilterDefMapping(Class<? extends TokenFilterFactory> factory, Map<String, Object> analyzerDef, SearchMapping mapping) {
		this.mapping = mapping;
		this.analyzerDef = analyzerDef;
		this.filter = SearchMapping.addElementToAnnotationArray( analyzerDef, "filters" );
		filter.put( "factory", factory );
	}

	/**
	 * {@code &#064;TokenFilterDef(, ... params={&#064;Parameter(name="name", value="value"), ...} }
	 */
	public TokenFilterDefMapping param(String name, String value) {
		Map<String, Object> param = SearchMapping.addElementToAnnotationArray(filter, "params");
		param.put("name", name);
		param.put("value", value);
		return this;
	}

	/**
	 * {@code &#064;TokenFilterDef(factory=factory)}
	 */
	public TokenFilterDefMapping filter(Class<? extends TokenFilterFactory> factory) {
		return new TokenFilterDefMapping(factory, analyzerDef, mapping );
	}

	public EntityMapping entity(Class<?> entityType) {
		return new EntityMapping(entityType,  mapping);
	}

	public AnalyzerDefMapping analyzerDef(String name, Class<? extends TokenizerFactory> tokenizerFactory) {
		return new AnalyzerDefMapping(name, tokenizerFactory, mapping);
	}

	public FullTextFilterDefMapping fullTextFilterDef(String name, Class<?> impl) {
		return new FullTextFilterDefMapping(mapping, name, impl );
	}

}
