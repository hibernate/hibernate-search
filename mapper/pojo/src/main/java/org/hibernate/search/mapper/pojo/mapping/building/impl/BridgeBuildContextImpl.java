/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.engine.common.spi.ServiceManager;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuildContext;

public class BridgeBuildContextImpl implements BridgeBuildContext {

	private final BuildContext buildContext;

	public BridgeBuildContextImpl(BuildContext buildContext) {
		this.buildContext = buildContext;
	}

	@Override
	public ServiceManager getServiceManager() {
		return buildContext.getServiceManager();
	}
}
