/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.metamodel.generator.model;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmMappingConfigurationContext;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;

public class MyConfigurer implements HibernateOrmSearchMappingConfigurer {
	@Override
	public void configure(HibernateOrmMappingConfigurationContext context) {
		ProgrammaticMappingConfigurationContext mapping = context.programmaticMapping();
		TypeMappingStep bookMapping = mapping.type( MyProgrammaticEntity.class );
		bookMapping.indexed();
		bookMapping.property( "text" )
				.fullTextField()
				.projectable( Projectable.YES );
		bookMapping.property( "number" )
				.genericField().sortable( Sortable.YES );
	}
}
