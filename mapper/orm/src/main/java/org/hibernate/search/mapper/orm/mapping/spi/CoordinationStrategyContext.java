/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.mapping.spi;

import org.hibernate.search.mapper.orm.coordination.common.spi.CoordinationStrategy;

public interface CoordinationStrategyContext {

	CoordinationStrategy coordinationStrategy();

}
