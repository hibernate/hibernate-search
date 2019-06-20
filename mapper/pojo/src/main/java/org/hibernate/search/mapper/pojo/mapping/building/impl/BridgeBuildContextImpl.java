/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.mapper.mapping.spi.MappingBuildContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.BridgeBuildContext;

public class BridgeBuildContextImpl implements BridgeBuildContext {

	private final MappingBuildContext buildContext;

	public BridgeBuildContextImpl(MappingBuildContext buildContext) {
		this.buildContext = buildContext;
	}

	@Override
	public BeanResolver getBeanResolver() {
		return buildContext.getBeanResolver();
	}
}
