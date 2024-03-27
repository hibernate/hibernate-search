/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.work.execution.spi;

public interface DocumentReferenceProvider {

	/**
	 * @return The document identifier.
	 */
	String identifier();

	/**
	 * @return The routing key.
	 */
	String routingKey();

	/**
	 * @return The entity identifier. Used when reporting failures.
	 */
	Object entityIdentifier();

}
