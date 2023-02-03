/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.mapping.impl;

import org.hibernate.search.engine.mapper.mapping.building.spi.MappingPartialBuildState;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.standalone.schema.management.impl.SchemaManagementListener;

public class StandalonePojoMappingPartialBuildState implements MappingPartialBuildState {

	private final PojoMappingDelegate mappingDelegate;
	private final StandalonePojoTypeContextContainer typeContextContainer;
	private final SchemaManagementListener schemaManagementListener;

	StandalonePojoMappingPartialBuildState(PojoMappingDelegate mappingDelegate,
			StandalonePojoTypeContextContainer typeContextContainer,
			SchemaManagementListener schemaManagementListener) {
		this.mappingDelegate = mappingDelegate;
		this.typeContextContainer = typeContextContainer;
		this.schemaManagementListener = schemaManagementListener;
	}

	@Override
	public void closeOnFailure() {
		mappingDelegate.close();
	}

	public StandalonePojoMapping finalizeMapping() {
		return new StandalonePojoMapping( mappingDelegate, typeContextContainer, schemaManagementListener );
	}

}
