/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.stub;

import org.hibernate.search.engine.mapper.mapping.building.spi.MappingKey;

public final class StubMappingKey implements MappingKey<StubMappingPartialBuildState, StubMappingImpl> {

	@Override
	public String render() {
		return "Stub mapping";
	}

}
