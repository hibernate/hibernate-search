/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.search.definition.impl;

import org.hibernate.search.mapper.pojo.model.path.spi.ProjectionConstructorPath;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.annotation.impl.SuppressForbiddenApis;

public class ConstructorProjectionApplicationException extends SearchException {

	private final ProjectionConstructorPath projectionConstructorPath;

	@SuppressForbiddenApis(reason = SEARCH_EXCEPTION_AND_SUBCLASSES_CAN_USE_CONSTRUCTOR)
	public ConstructorProjectionApplicationException(String message,
			Throwable cause,
			ProjectionConstructorPath projectionConstructorPath) {
		super( message + "\n" + projectionConstructorPath.toPrefixedString(), cause );
		this.projectionConstructorPath = projectionConstructorPath;
	}

	public ProjectionConstructorPath projectionConstructorPath() {
		return projectionConstructorPath;
	}
}
