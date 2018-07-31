/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.spi;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaContext;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * A helper for backends, making it easier to return accessors before they are completely defined.
 */
public final class IndexSchemaFieldDefinitionHelper<F> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final IndexSchemaContext schemaContext;

	private final DeferredInitializationIndexFieldAccessor<F> rawAccessor =
			new DeferredInitializationIndexFieldAccessor<>();

	private boolean accessorCreated = false;

	public IndexSchemaFieldDefinitionHelper(IndexSchemaContext schemaContext) {
		this.schemaContext = schemaContext;
	}

	public IndexSchemaContext getSchemaContext() {
		return schemaContext;
	}

	/**
	 * @return A (potentially un-{@link #initialize(IndexFieldAccessor) initialized}) accessor
	 */
	public IndexFieldAccessor<F> createAccessor() {
		if ( accessorCreated ) {
			throw log.cannotCreateAccessorMultipleTimes( schemaContext.getEventContext() );
		}
		accessorCreated = true;
		return rawAccessor;
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
		if ( !accessorCreated ) {
			throw log.incompleteFieldDefinition( schemaContext.getEventContext() );
		}
		rawAccessor.initialize( delegate );
	}
}
