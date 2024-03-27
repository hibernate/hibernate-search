/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.stub;

import org.hibernate.search.engine.mapper.model.spi.MappingElement;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.reporting.EventContext;

public class StubMappingElement implements MappingElement {
	@Override
	public EventContext eventContext() {
		return EventContexts.defaultContext();
	}
}
