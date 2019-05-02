/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.index.spi.DocumentRefreshStrategy;
import org.hibernate.search.util.common.impl.Futures;

public interface AutomaticIndexingSynchronizationStrategy {

	DocumentRefreshStrategy getDocumentRefreshStrategy();

	void handleFuture(CompletableFuture<?> future);

	AutomaticIndexingSynchronizationStrategy QUEUED = new AutomaticIndexingSynchronizationStrategy() {
		@Override
		public DocumentRefreshStrategy getDocumentRefreshStrategy() {
			return DocumentRefreshStrategy.NONE;
		}

		@Override
		public void handleFuture(CompletableFuture<?> future) {
			// Nothing to do: works are queued, we're fine.
		}
	};

	AutomaticIndexingSynchronizationStrategy COMMITTED = new AutomaticIndexingSynchronizationStrategy() {
		@Override
		public DocumentRefreshStrategy getDocumentRefreshStrategy() {
			return DocumentRefreshStrategy.NONE;
		}

		@Override
		public void handleFuture(CompletableFuture<?> future) {
			Futures.unwrappedExceptionJoin( future );
		}
	};

	AutomaticIndexingSynchronizationStrategy SEARCHABLE = new AutomaticIndexingSynchronizationStrategy() {
		@Override
		public DocumentRefreshStrategy getDocumentRefreshStrategy() {
			return DocumentRefreshStrategy.FORCE;
		}

		@Override
		public void handleFuture(CompletableFuture<?> future) {
			Futures.unwrappedExceptionJoin( future );
		}
	};

}
