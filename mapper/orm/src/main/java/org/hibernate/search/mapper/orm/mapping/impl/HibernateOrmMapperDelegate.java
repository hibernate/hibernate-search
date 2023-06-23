/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import org.hibernate.search.engine.backend.reporting.spi.BackendMappingHints;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.orm.coordination.common.spi.CoordinationStrategy;
import org.hibernate.search.mapper.orm.model.impl.HibernateOrmBasicTypeMetadataProvider;
import org.hibernate.search.mapper.orm.reporting.impl.HibernateOrmMappingHints;
import org.hibernate.search.mapper.orm.session.impl.ConfiguredAutomaticIndexingStrategy;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoContainedTypeExtendedMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexedTypeExtendedMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMapperDelegate;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.util.common.impl.Closer;

public final class HibernateOrmMapperDelegate
		implements PojoMapperDelegate<HibernateOrmMappingPartialBuildState> {

	private final HibernateOrmTypeContextContainer.Builder typeContextContainerBuilder;
	private final BeanHolder<? extends CoordinationStrategy> coordinationStrategyHolder;
	private final ConfiguredAutomaticIndexingStrategy configuredAutomaticIndexingStrategy;

	HibernateOrmMapperDelegate(HibernateOrmBasicTypeMetadataProvider basicTypeMetadataProvider,
			BeanHolder<? extends CoordinationStrategy> coordinationStrategyHolder,
			ConfiguredAutomaticIndexingStrategy configuredAutomaticIndexingStrategy) {
		typeContextContainerBuilder = new HibernateOrmTypeContextContainer.Builder( basicTypeMetadataProvider );
		this.coordinationStrategyHolder = coordinationStrategyHolder;
		this.configuredAutomaticIndexingStrategy = configuredAutomaticIndexingStrategy;
	}

	@Override
	public void closeOnFailure() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( ConfiguredAutomaticIndexingStrategy::stop, configuredAutomaticIndexingStrategy );
			closer.push( CoordinationStrategy::stop, coordinationStrategyHolder, BeanHolder::get );
			closer.push( BeanHolder::close, coordinationStrategyHolder );
		}
	}

	@Override
	public <E> PojoIndexedTypeExtendedMappingCollector createIndexedTypeExtendedMappingCollector(
			PojoRawTypeModel<E> rawTypeModel, String entityName) {
		return typeContextContainerBuilder.addIndexed( rawTypeModel, entityName );
	}

	@Override
	public <E> PojoContainedTypeExtendedMappingCollector createContainedTypeExtendedMappingCollector(
			PojoRawTypeModel<E> rawTypeModel, String entityName) {
		return typeContextContainerBuilder.addContained( rawTypeModel, entityName );
	}

	@Override
	public HibernateOrmMappingPartialBuildState prepareBuild(PojoMappingDelegate mappingDelegate) {
		return new HibernateOrmMappingPartialBuildState( mappingDelegate, typeContextContainerBuilder,
				coordinationStrategyHolder, configuredAutomaticIndexingStrategy );
	}

	@Override
	public BackendMappingHints hints() {
		return HibernateOrmMappingHints.INSTANCE;
	}
}
