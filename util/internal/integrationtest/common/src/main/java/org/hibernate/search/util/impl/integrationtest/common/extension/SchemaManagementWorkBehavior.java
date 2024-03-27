/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.extension;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;

public interface SchemaManagementWorkBehavior {

	CompletableFuture<?> execute(ContextualFailureCollector failureCollector);

}
