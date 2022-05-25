/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.mapping.impl;

import org.hibernate.search.engine.mapper.mapping.building.spi.MappingPartialBuildState;
import org.hibernate.search.mapper.javabean.schema.management.impl.SchemaManagementListener;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;

public class JavaBeanMappingPartialBuildState implements MappingPartialBuildState {

	private final PojoMappingDelegate mappingDelegate;
	private final JavaBeanTypeContextContainer typeContextContainer;
	private final SchemaManagementListener schemaManagementListener;

	JavaBeanMappingPartialBuildState(PojoMappingDelegate mappingDelegate,
			JavaBeanTypeContextContainer typeContextContainer,
			SchemaManagementListener schemaManagementListener) {
		this.mappingDelegate = mappingDelegate;
		this.typeContextContainer = typeContextContainer;
		this.schemaManagementListener = schemaManagementListener;
	}

	@Override
	public void closeOnFailure() {
		mappingDelegate.close();
	}

	public JavaBeanMapping finalizeMapping() {
		return new JavaBeanMapping( mappingDelegate, typeContextContainer, schemaManagementListener );
	}

}
