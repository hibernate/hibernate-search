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

import java.util.HashMap;
import java.util.Map;

import org.apache.solr.analysis.TokenizerFactory;
import org.apache.solr.analysis.TokenFilterFactory;

/**
 * @author Emmanuel Bernard
 */
public class AnalyzerDefMapping {
	private SearchMapping mapping;
	private Map<String, Object> analyzerDef;
	private Map<String, Object> tokenizer;

	AnalyzerDefMapping(String name, Class<? extends TokenizerFactory> tokenizerFactory, SearchMapping mapping) {
		this.mapping = mapping;
		this.analyzerDef = new HashMap<String, Object>();
		mapping.addAnalyzerDef( analyzerDef );
		analyzerDef.put( "name", name );
		tokenizer = new HashMap<String, Object>();
		tokenizer.put( "factory", tokenizerFactory );
		analyzerDef.put( "tokenizer", tokenizer );
	}

	/**
	 * {@code &#064;TokenizerDef(, ... params={&#064;Parameter(name="name", value="value"), ...}) }
	 */
	public AnalyzerDefMapping tokenizerParam(String name, String value) {
		Map<String, Object> param = SearchMapping.addElementToAnnotationArray( tokenizer, "params" );
		param.put( "name", name );
		param.put( "value", value );
		return this;
	}

	/**
	 * {@code &#064;TokenFilterDef(factory=factory) }
	 */
	public TokenFilterDefMapping filter(Class<? extends TokenFilterFactory> factory) {
		return new TokenFilterDefMapping( factory, analyzerDef, mapping );
	}

	public AnalyzerDefMapping analyzerDef(String name, Class<? extends TokenizerFactory> tokenizerFactory) {
		return new AnalyzerDefMapping( name, tokenizerFactory, mapping );
	}

	public EntityMapping entity(Class<?> entityType) {
		return new EntityMapping( entityType, mapping );
	}

}
