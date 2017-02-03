/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.definition.impl;

import org.apache.lucene.analysis.util.TokenizerFactory;
import org.hibernate.annotations.common.annotationfactory.AnnotationDescriptor;
import org.hibernate.annotations.common.annotationfactory.AnnotationFactory;
import org.hibernate.search.annotations.TokenizerDef;


/**
 * @author Yoann Rodiere
 */
public class LuceneTokenizerDefinitionContextImpl implements LuceneAnalysisDefinitionBuilder<TokenizerDef> {

	private Class<? extends TokenizerFactory> factoryClass;

	private final ParametersBuilder params = new ParametersBuilder();

	public void factory(Class<? extends TokenizerFactory> factoryClass) {
		this.factoryClass = factoryClass;
	}

	public void param(String name, String value) {
		params.put( name, value );
	}

	@Override
	public TokenizerDef build() {
		AnnotationDescriptor descriptor = new AnnotationDescriptor( TokenizerDef.class );
		descriptor.setValue( "factory", factoryClass );
		descriptor.setValue( "params", params.build() );
		return AnnotationFactory.create( descriptor );
	}

}
