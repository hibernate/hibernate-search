/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.document.model.dsl.spi;

import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.reporting.EventContext;

public interface IndexSchemaBuildContext {

	/**
	 * @return A list of failure context elements to be passed to the constructor of any
	 * {@link SearchException} occurring in this context.
	 */
	EventContext eventContext();

}
