/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoContainedTypeExtendedMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexedTypeExtendedMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMapperDelegate;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

public final class HibernateOrmMapperDelegate
		implements PojoMapperDelegate<HibernateOrmMappingPartialBuildState> {

	private final HibernateOrmTypeContextContainer.Builder typeContextContainerBuilder =
			new HibernateOrmTypeContextContainer.Builder();

	HibernateOrmMapperDelegate() {
	}

	@Override
	public void closeOnFailure() {
		// Nothing to do
	}

	@Override
	public <E> PojoIndexedTypeExtendedMappingCollector createIndexedTypeExtendedMappingCollector(
			PojoRawTypeModel<E> rawTypeModel, String indexName) {
		return typeContextContainerBuilder.addIndexed( rawTypeModel, indexName );
	}

	@Override
	public <E> PojoContainedTypeExtendedMappingCollector createContainedTypeExtendedMappingCollector(
			PojoRawTypeModel<E> rawTypeModel) {
		return typeContextContainerBuilder.addContained( rawTypeModel );
	}

	@Override
	public HibernateOrmMappingPartialBuildState prepareBuild(PojoMappingDelegate mappingDelegate) {
		return new HibernateOrmMappingPartialBuildState( mappingDelegate, typeContextContainerBuilder );
	}
}
