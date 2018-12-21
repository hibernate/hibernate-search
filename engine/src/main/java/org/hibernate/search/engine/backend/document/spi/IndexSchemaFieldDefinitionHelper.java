/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.spi;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaBuildContext;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * A helper for backends, making it easier to return accessors before they are completely defined.
 */
public final class IndexSchemaFieldDefinitionHelper<F> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final IndexSchemaBuildContext schemaContext;

	private final DeferredInitializationIndexFieldAccessor<F> deferredInitializationAccessor =
			new DeferredInitializationIndexFieldAccessor<>();

	private boolean accessorCreated = false;

	public IndexSchemaFieldDefinitionHelper(IndexSchemaBuildContext schemaContext) {
		this.schemaContext = schemaContext;
	}

	/**
	 * @return A (potentially un-{@link #initialize(IndexFieldAccessor) initialized}) accessor
	 */
	public IndexFieldAccessor<F> createAccessor() {
		if ( accessorCreated ) {
			throw log.cannotCreateAccessorMultipleTimes( schemaContext.getEventContext() );
		}
		accessorCreated = true;
		return deferredInitializationAccessor;
	}

	/**
	 * Initialize the field definition, enabling writes to an underlying field.
	 * <p>
	 * This method may or may not be called during bootstrap; if it isn't called,
	 * writes triggered by the mapper through the accessor won't have any effect.
	 *
	 * @param delegate The delegate to use when writing to the accessor returned by {@link #createAccessor()}.
	 */
	public void initialize(IndexFieldAccessor<F> delegate) {
		checkAccessorCreated();
		deferredInitializationAccessor.initialize( delegate );
	}

	private void checkAccessorCreated() {
		if ( !accessorCreated ) {
			throw log.incompleteFieldDefinition( schemaContext.getEventContext() );
		}
	}
}
