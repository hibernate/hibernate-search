/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.spi;

public interface ContextualErrorHandler {

	void markAsFailed(Object workInfo, Throwable throwable);

	void markAsSkipped(Object workInfo);

	void addThrowable(Throwable throwable);

	void handle();

}
