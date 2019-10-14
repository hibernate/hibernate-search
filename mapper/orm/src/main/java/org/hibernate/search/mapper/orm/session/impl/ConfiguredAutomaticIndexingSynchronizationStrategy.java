/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session.impl;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.orm.session.AutomaticIndexingSynchronizationConfigurationContext;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;
import org.hibernate.search.util.common.impl.Contracts;

public class ConfiguredAutomaticIndexingSynchronizationStrategy {

	private final DocumentCommitStrategy documentCommitStrategy;
	private final DocumentRefreshStrategy documentRefreshStrategy;
	private final Consumer<CompletableFuture<?>> indexingFutureHandler;

	private ConfiguredAutomaticIndexingSynchronizationStrategy(Builder configurationContext) {
		this.documentCommitStrategy = configurationContext.documentCommitStrategy;
		this.documentRefreshStrategy = configurationContext.documentRefreshStrategy;
		this.indexingFutureHandler = configurationContext.indexingFutureHandler;
	}

	DocumentCommitStrategy getDocumentCommitStrategy() {
		return documentCommitStrategy;
	}

	DocumentRefreshStrategy getDocumentRefreshStrategy() {
		return documentRefreshStrategy;
	}

	public void executeAndSynchronize(PojoIndexingPlan indexingPlan) {
		indexingFutureHandler.accept( indexingPlan.execute() );
	}

	public static class Builder
			implements AutomaticIndexingSynchronizationConfigurationContext {

		private DocumentCommitStrategy documentCommitStrategy = DocumentCommitStrategy.NONE;
		private DocumentRefreshStrategy documentRefreshStrategy = DocumentRefreshStrategy.NONE;
		private Consumer<CompletableFuture<?>> indexingFutureHandler = future -> {
		};

		@Override
		public void documentCommitStrategy(DocumentCommitStrategy strategy) {
			Contracts.assertNotNull( strategy, "strategy" );
			this.documentCommitStrategy = strategy;
		}

		@Override
		public void documentRefreshStrategy(DocumentRefreshStrategy strategy) {
			Contracts.assertNotNull( strategy, "strategy" );
			this.documentRefreshStrategy = strategy;
		}

		@Override
		public void indexingFutureHandler(Consumer<CompletableFuture<?>> handler) {
			Contracts.assertNotNull( handler, "handler" );
			this.indexingFutureHandler = handler;
		}

		public ConfiguredAutomaticIndexingSynchronizationStrategy build() {
			return new ConfiguredAutomaticIndexingSynchronizationStrategy( this );
		}
	}

}
