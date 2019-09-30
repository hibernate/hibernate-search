/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.mapper;

import org.hibernate.search.engine.mapper.mapping.building.spi.MappingKey;

public final class StubMappingKey implements MappingKey<StubMappingPartialBuildState, StubMapping> {

	@Override
	public String render() {
		return "Stub mapping";
	}

}
