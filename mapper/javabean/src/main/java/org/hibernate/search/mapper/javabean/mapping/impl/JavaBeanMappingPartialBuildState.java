/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.mapping.impl;

import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.engine.mapper.mapping.spi.MappingPartialBuildState;
import org.hibernate.search.mapper.javabean.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;

public class JavaBeanMappingPartialBuildState implements MappingPartialBuildState {

	private final PojoMappingDelegate mappingDelegate;
	private final JavaBeanTypeContextContainer typeContextContainer;

	JavaBeanMappingPartialBuildState(PojoMappingDelegate mappingDelegate,
			JavaBeanTypeContextContainer typeContextContainer) {
		this.mappingDelegate = mappingDelegate;
		this.typeContextContainer = typeContextContainer;
	}

	@Override
	public void closeOnFailure() {
		mappingDelegate.close();
	}

	public MappingImplementor<SearchMapping> finalizeMapping() {
		return new JavaBeanMapping( mappingDelegate, typeContextContainer );
	}

}
