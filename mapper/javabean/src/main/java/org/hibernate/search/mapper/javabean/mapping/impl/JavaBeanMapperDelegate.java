/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.mapping.impl;

import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoContainedTypeExtendedMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexedTypeExtendedMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMapperDelegate;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

public final class JavaBeanMapperDelegate
		implements PojoMapperDelegate<JavaBeanMappingPartialBuildState> {

	private final JavaBeanTypeContextContainer.Builder typeContextContainerBuilder =
			new JavaBeanTypeContextContainer.Builder();

	@Override
	public void closeOnFailure() {
		// Nothing to do
	}

	@Override
	public <E> PojoIndexedTypeExtendedMappingCollector createIndexedTypeExtendedMappingCollector(
			PojoRawTypeModel<E> rawTypeModel, String entityName, String indexName) {
		return typeContextContainerBuilder.addIndexed( rawTypeModel, entityName );
	}

	@Override
	public <E> PojoContainedTypeExtendedMappingCollector createContainedTypeExtendedMappingCollector(
			PojoRawTypeModel<E> rawTypeModel, String entityName) {
		// This is a placeholder: we don't care about contained types at the moment.
		return new JavaBeanContainedTypeContext.Builder<>();
	}

	@Override
	public JavaBeanMappingPartialBuildState prepareBuild(PojoMappingDelegate mappingDelegate) {
		return new JavaBeanMappingPartialBuildState( mappingDelegate, typeContextContainerBuilder.build() );
	}

}
