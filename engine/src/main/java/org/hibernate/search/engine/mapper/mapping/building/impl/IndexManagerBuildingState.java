/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.mapper.mapping.building.impl;

import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexRootBuilder;
import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;

public interface IndexManagerBuildingState {

	IndexRootBuilder getSchemaRootNodeBuilder();

	IndexManagerImplementor build();

}
