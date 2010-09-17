/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 *  Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 *  indicated by the @author tags or express copyright attribution
 *  statements applied by the authors.  All third-party contributions are
 *  distributed under license by Red Hat, Inc.
 *
 *  This copyrighted material is made available to anyone wishing to use, modify,
 *  copy, or redistribute it subject to the terms and conditions of the GNU
 *  Lesser General Public License, as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this distribution; if not, write to:
 *  Free Software Foundation, Inc.
 *  51 Franklin Street, Fifth Floor
 *  Boston, MA  02110-1301  USA
 */
package org.hibernate.search.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.util.Version;
import org.apache.solr.analysis.CharFilterFactory;
import org.apache.solr.analysis.TokenFilterFactory;
import org.apache.solr.analysis.TokenizerChain;
import org.apache.solr.analysis.TokenizerFactory;
import org.apache.solr.common.ResourceLoader;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.util.plugin.ResourceLoaderAware;

import org.hibernate.search.SearchException;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.CharFilterDef;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;
import org.hibernate.search.util.HibernateSearchResourceLoader;

/**
 * Instances of this class are used to build Lucene analyzers which are defined using the solr <code>TokenFilterFactory</code>.
 * To make the dependency to the solr framework optional only this class has direct dependecies to solr. Solr dependencies
 * are not supposed to be used anywhere else (except the actual configuration of the analyzers in the domain model).
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
final class SolrAnalyzerBuilder {
	private SolrAnalyzerBuilder() {
	}

	/**
	 * Builds a Lucene <code>Analyzer</code> from the specified <code>AnalyzerDef</code> annotation.
	 *
	 * @param analyzerDef The <code>AnalyzerDef</code> annotation as found in the annotated domain class.
	 *
	 * @return a Lucene <code>Analyzer</code>
	 */
	public static Analyzer buildAnalyzer(AnalyzerDef analyzerDef) {
		TokenizerDef token = analyzerDef.tokenizer();
		TokenizerFactory tokenFactory = ( TokenizerFactory ) instantiate( token.factory() );
		tokenFactory.init( getMapOfParameters( token.params() ) );

		final int length = analyzerDef.filters().length;
		final int charLength = analyzerDef.charFilters().length;
		TokenFilterFactory[] filters = new TokenFilterFactory[length];
		CharFilterFactory[] charFilters = new CharFilterFactory[charLength];
		ResourceLoader resourceLoader = new HibernateSearchResourceLoader();
		for ( int index = 0; index < length; index++ ) {
			TokenFilterDef filterDef = analyzerDef.filters()[index];
			filters[index] = ( TokenFilterFactory ) instantiate( filterDef.factory() );
			filters[index].init( getMapOfParameters( filterDef.params() ) );
			if ( filters[index] instanceof ResourceLoaderAware ) {
				( ( ResourceLoaderAware ) filters[index] ).inform( resourceLoader );
			}
		}
		for ( int index = 0; index < charFilters.length; index++ ) {
			CharFilterDef charFilterDef = analyzerDef.charFilters()[index];
			charFilters[index] = ( CharFilterFactory ) instantiate( charFilterDef.factory() );
			charFilters[index].init( getMapOfParameters( charFilterDef.params() ) );
			if ( charFilters[index] instanceof ResourceLoaderAware ) {
				( ( ResourceLoaderAware ) charFilters[index] ).inform( resourceLoader );
			}
		}
		return new TokenizerChain( charFilters, tokenFactory, filters );
	}

	private static Object instantiate(Class clazz) {
		try {
			return clazz.newInstance();
		}
		catch ( IllegalAccessException e ) {
			throw new SearchException( "Unable to instantiate class: " + clazz, e );
		}
		catch ( InstantiationException e ) {
			throw new SearchException( "Unable to instantiate class: " + clazz, e );
		}
		catch ( Throwable e) {
			throw new SearchException( "foo");
		}
	}

	private static Map<String, String> getMapOfParameters(Parameter[] params) {
		Map<String, String> mapOfParams = new HashMap<String, String>( params.length );
		for ( Parameter param : params ) {
			mapOfParams.put( param.name(), param.value() );
		}
		mapOfParams.put( IndexSchema.LUCENE_MATCH_VERSION_PARAM, Version.LUCENE_30.toString() );
		return Collections.unmodifiableMap( mapOfParams );
	}
}
