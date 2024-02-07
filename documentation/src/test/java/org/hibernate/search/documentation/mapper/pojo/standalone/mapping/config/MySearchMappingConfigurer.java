/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
		context.addEntityType( Book.class ); // <1>

		context.annotationMapping() // <2>
				.discoverAnnotationsFromReferencedTypes( false )
				.discoverAnnotatedTypesFromRootMappingAnnotations( false );

		ProgrammaticMappingConfigurationContext mappingContext = context.programmaticMapping(); // <3>
		TypeMappingStep bookMapping = mappingContext.type( Book.class ); // <4>
		bookMapping.indexed(); // <5>
		bookMapping.property( "id" ) // <6>
				.documentId(); // <7>
		bookMapping.property( "title" ) // <8>
				.fullTextField().analyzer( "english" ); // <9>
	}
}
//end::include[]
