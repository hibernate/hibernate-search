/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.engine.mapper.mapping.spi.MappingPartialBuildState;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmAutomaticIndexingSynchronizationStrategyName;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.session.impl.AutomaticIndexingSynchronizationStrategy;
import org.hibernate.search.mapper.orm.mapping.spi.HibernateOrmMapping;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.util.common.AssertionFailure;

public class HibernateOrmMappingPartialBuildState implements MappingPartialBuildState {

	private static final ConfigurationProperty<HibernateOrmAutomaticIndexingSynchronizationStrategyName> AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.Radicals.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY )
					.as( HibernateOrmAutomaticIndexingSynchronizationStrategyName.class, HibernateOrmAutomaticIndexingSynchronizationStrategyName::of )
					.withDefault( HibernateOrmMapperSettings.Defaults.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY )
					.build();

	private final PojoMappingDelegate mappingDelegate;

	public HibernateOrmMappingPartialBuildState(PojoMappingDelegate mappingDelegate) {
		this.mappingDelegate = mappingDelegate;
	}

	public MappingImplementor<HibernateOrmMapping> bindToSessionFactory(
			SessionFactoryImplementor sessionFactoryImplementor,
			ConfigurationPropertySource propertySource) {
		AutomaticIndexingSynchronizationStrategy synchronizationStrategy =
				getAutomaticIndexingSynchronizationStrategy( propertySource );
		return new HibernateOrmMappingImpl( mappingDelegate, sessionFactoryImplementor, synchronizationStrategy );
	}

	@Override
	public void closeOnFailure() {
		mappingDelegate.close();
	}


	private AutomaticIndexingSynchronizationStrategy getAutomaticIndexingSynchronizationStrategy(ConfigurationPropertySource propertySource) {
		HibernateOrmAutomaticIndexingSynchronizationStrategyName name =
				AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY.get( propertySource );
		switch ( name ) {
			case QUEUED:
				return AutomaticIndexingSynchronizationStrategy.QUEUED;
			case COMMITTED:
				return AutomaticIndexingSynchronizationStrategy.COMMITTED;
			case SEARCHABLE:
				return AutomaticIndexingSynchronizationStrategy.SEARCHABLE;
			default:
				throw new AssertionFailure( "Unexpected automatic indexing synchronization strategy name: " + name );
		}
	}
}
