/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

public interface MappingPartialBuildState {

	/**
	 * Close the resources held by this object.
	 * <p>
	 * Called in the event of a failure that will prevent the mapping to be fully built.
	 */
	void closeOnFailure();

}
