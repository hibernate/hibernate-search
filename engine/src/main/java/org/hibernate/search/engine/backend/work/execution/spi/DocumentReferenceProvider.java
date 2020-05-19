/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
