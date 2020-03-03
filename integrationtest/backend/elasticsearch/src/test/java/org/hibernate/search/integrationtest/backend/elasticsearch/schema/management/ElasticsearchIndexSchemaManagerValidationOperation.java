/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.schema.management.spi.IndexSchemaManager;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.reporting.spi.RootFailureCollector;
import org.hibernate.search.util.common.impl.Futures;

/**
 * Different validation operations to be executed on a schema.
 * <p>
 * Used in tests that check the result of validation.
 */
enum ElasticsearchIndexSchemaManagerValidationOperation {

	CREATE_OR_VALIDATE {
		@Override
		protected CompletableFuture<?> apply(IndexSchemaManager schemaManager, ContextualFailureCollector failureCollector) {
			return schemaManager.createOrValidate( failureCollector );
		}
	},
	VALIDATE {
		@Override
		protected CompletableFuture<?> apply(IndexSchemaManager schemaManager, ContextualFailureCollector failureCollector) {
			return schemaManager.validate( failureCollector );
		}
	};

	public final CompletableFuture<?> apply(IndexSchemaManager schemaManager) {
		RootFailureCollector failureCollector = new RootFailureCollector( "validation" );
		ContextualFailureCollector validationFailureCollector =
				failureCollector.withContext( EventContexts.schemaValidation() );
		return apply( schemaManager, validationFailureCollector )
				.handle( Futures.handler( (ignored, t) -> {
					if ( t != null ) {
						validationFailureCollector.add( t );
					}
					failureCollector.checkNoFailure();
					return null;
				} ) );
	}

	protected abstract CompletableFuture<?> apply(IndexSchemaManager schemaManager, ContextualFailureCollector failureCollector);

	public static EnumSet<ElasticsearchIndexSchemaManagerValidationOperation> all() {
		return EnumSet.allOf( ElasticsearchIndexSchemaManagerValidationOperation.class );
	}

}
