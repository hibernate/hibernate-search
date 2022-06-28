/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.pojo.standalone.mapping.config;

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
