/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.mapping.impl;

import org.hibernate.search.engine.backend.reporting.spi.BackendMappingHints;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoContainedTypeExtendedMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexedTypeExtendedMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMapperDelegate;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.standalone.mapping.metadata.impl.StandalonePojoEntityTypeMetadataProvider;
import org.hibernate.search.mapper.pojo.standalone.reporting.impl.StandalonePojoMappingHints;
import org.hibernate.search.mapper.pojo.standalone.schema.management.impl.SchemaManagementListener;

public final class StandalonePojoMapperDelegate
		implements PojoMapperDelegate<StandalonePojoMappingPartialBuildState> {

	private final StandalonePojoEntityTypeMetadataProvider metadataProvider;
	private final StandalonePojoTypeContextContainer.Builder typeContextContainerBuilder =
			new StandalonePojoTypeContextContainer.Builder();
	private final SchemaManagementListener schemaManagementListener;

	public StandalonePojoMapperDelegate(StandalonePojoEntityTypeMetadataProvider metadataProvider,
			SchemaManagementListener schemaManagementListener) {
		this.metadataProvider = metadataProvider;
		this.schemaManagementListener = schemaManagementListener;
	}

	@Override
	public void closeOnFailure() {
		// Nothing to do
	}

	@Override
	public <E> PojoIndexedTypeExtendedMappingCollector createIndexedTypeExtendedMappingCollector(
			PojoRawTypeModel<E> rawTypeModel, String entityName) {
		return typeContextContainerBuilder.addIndexed( rawTypeModel, entityName, metadataProvider.get( rawTypeModel ) );
	}

	@Override
	public <E> PojoContainedTypeExtendedMappingCollector createContainedTypeExtendedMappingCollector(
			PojoRawTypeModel<E> rawTypeModel, String entityName) {
		return typeContextContainerBuilder.addContained( rawTypeModel, entityName, metadataProvider.get( rawTypeModel ) );
	}

	@Override
	public StandalonePojoMappingPartialBuildState prepareBuild(PojoMappingDelegate mappingDelegate) {
		return new StandalonePojoMappingPartialBuildState( mappingDelegate, typeContextContainerBuilder.build(),
				schemaManagementListener );
	}

	@Override
	public BackendMappingHints hints() {
		return StandalonePojoMappingHints.INSTANCE;
	}
}
