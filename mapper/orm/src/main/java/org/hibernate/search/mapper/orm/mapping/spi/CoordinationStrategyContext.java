package org.hibernate.search.mapper.orm.mapping.spi;

import org.hibernate.search.mapper.orm.coordination.common.spi.CoordinationStrategy;

public interface CoordinationStrategyContext {

	CoordinationStrategy coordinationStrategy();

}
