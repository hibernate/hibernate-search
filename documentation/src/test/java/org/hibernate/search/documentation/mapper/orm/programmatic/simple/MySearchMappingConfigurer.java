/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.programmatic.simple;

import org.hibernate.search.mapper.orm.mapping.HibernateOrmMappingConfigurationContext;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;

//tag::include[]
public class MySearchMappingConfigurer implements HibernateOrmSearchMappingConfigurer {
	@Override
	public void configure(HibernateOrmMappingConfigurationContext context) {
		ProgrammaticMappingConfigurationContext mapping = context.programmaticMapping(); // <1>
		TypeMappingStep bookMapping = mapping.type( Book.class ); // <2>
		bookMapping.indexed(); // <3>
		bookMapping.property( "title" ) // <4>
				.fullTextField().analyzer( "english" ); // <5>
	}
}
//end::include[]
