/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.document.model.dsl.spi;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * Opens an extension point to accept implicit fields.
 */
@Incubating
public interface ImplicitFieldContributor {
	void contribute(ImplicitFieldCollector collector);
}
