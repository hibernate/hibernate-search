/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.pojo.standalone.mapping.config;

import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurer;

//tag::include[]
public class MySearchMappingConfigurer implements StandalonePojoMappingConfigurer {
	@Override
	public void configure(StandalonePojoMappingConfigurationContext context) {
		context.annotationMapping() // <1>
				.discoverAnnotationsFromReferencedTypes( false )
				.discoverAnnotatedTypesFromRootMappingAnnotations( false );

		ProgrammaticMappingConfigurationContext mappingContext = context.programmaticMapping(); // <2>
		TypeMappingStep bookMapping = mappingContext.type( Book.class ); // <3>
		bookMapping.searchEntity(); // <4>
		bookMapping.indexed(); // <5>
		bookMapping.property( "id" ) // <6>
				.documentId(); // <7>
		bookMapping.property( "title" ) // <8>
				.fullTextField().analyzer( "english" ); // <9>
	}
}
//end::include[]
