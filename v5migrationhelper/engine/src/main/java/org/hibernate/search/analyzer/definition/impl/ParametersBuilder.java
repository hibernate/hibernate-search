/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.definition.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.annotations.common.annotationfactory.AnnotationDescriptor;
import org.hibernate.annotations.common.annotationfactory.AnnotationFactory;
import org.hibernate.search.annotations.Parameter;


/**
 * @author Yoann Rodiere
 */
class ParametersBuilder implements LuceneAnalysisDefinitionBuilder<Parameter[]> {

	private final Map<String, String> params = new LinkedHashMap<>();

	public void put(String name, String value) {
		params.put( name, value );
	}

	@Override
	public Parameter[] build() {
		AnnotationDescriptor descriptor = new AnnotationDescriptor( Parameter.class );
		Parameter[] result = new Parameter[params.size()];

		int index = 0;
		for ( Map.Entry<String, String> entry : params.entrySet() ) {
			descriptor.setValue( "name" , entry.getKey() );
			descriptor.setValue( "value", entry.getValue() );
			result[index] = AnnotationFactory.create( descriptor );
			++index;
		}

		return result;
	}

}
