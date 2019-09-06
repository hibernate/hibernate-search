/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.engine.mapper.mapping.spi.MappingFinalizationContext;
import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.engine.mapper.mapping.spi.MappingPartialBuildState;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;

public class HibernateOrmMappingPartialBuildState implements MappingPartialBuildState {

	private final PojoMappingDelegate mappingDelegate;
	private final HibernateOrmTypeContextContainer.Builder typeContextContainerBuilder;

	HibernateOrmMappingPartialBuildState(PojoMappingDelegate mappingDelegate,
			HibernateOrmTypeContextContainer.Builder typeContextContainerBuilder) {
		this.mappingDelegate = mappingDelegate;
		this.typeContextContainerBuilder = typeContextContainerBuilder;
	}

	public MappingImplementor<HibernateOrmMapping> bindToSessionFactory(
			MappingFinalizationContext context,
			SessionFactoryImplementor sessionFactoryImplementor) {
		return HibernateOrmMapping.create(
				mappingDelegate, typeContextContainerBuilder.build( sessionFactoryImplementor ),
				sessionFactoryImplementor,
				context.getConfigurationPropertySource()
		);
	}

	@Override
	public void closeOnFailure() {
		mappingDelegate.close();
	}

}
