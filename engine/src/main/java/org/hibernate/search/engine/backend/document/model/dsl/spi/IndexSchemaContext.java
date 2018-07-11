/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.dsl.spi;

import org.hibernate.search.engine.logging.spi.FailureContext;

public interface IndexSchemaContext {

	/**
	 * @return A list of failure context elements to be passed to the constructor of any
	 * {@link org.hibernate.search.engine.logging.spi.SearchExceptionWithContext} occurring in this context.
	 */
	FailureContext getFailureContext();

}
