/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.spi;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.converter.FromIndexFieldValueConverter;
import org.hibernate.search.engine.backend.document.converter.ToIndexFieldValueConverter;
import org.hibernate.search.engine.backend.document.converter.spi.PassThroughToIndexFieldValueConverter;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaContext;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.impl.common.Contracts;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * A helper for backends, making it easier to return accessors before they are completely defined,
 * and creating the user-defined converter to apply in search queries.
 */
public final class IndexSchemaFieldDefinitionHelper<F> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final IndexSchemaContext schemaContext;

	private final DeferredInitializationIndexFieldAccessor<F> deferredInitializationAccessor =
			new DeferredInitializationIndexFieldAccessor<>();

	private ToIndexFieldValueConverter<?, ? extends F> dslToIndexConverter;
	private FromIndexFieldValueConverter<? super F, ?> projectionFromIndexConverter;

	private boolean accessorCreated = false;

	public IndexSchemaFieldDefinitionHelper(IndexSchemaContext schemaContext,
			Class<F> indexFieldType) {
		this( schemaContext, new PassThroughToIndexFieldValueConverter<>( indexFieldType ) );
	}

	public IndexSchemaFieldDefinitionHelper(IndexSchemaContext schemaContext,
			ToIndexFieldValueConverter<F, ? extends F> identityToIndexConverter) {
		this.schemaContext = schemaContext;
		this.dslToIndexConverter = identityToIndexConverter;
		this.projectionFromIndexConverter = null;
	}

	public IndexSchemaContext getSchemaContext() {
		return schemaContext;
	}

	public void dslConverter(ToIndexFieldValueConverter<?, ? extends F> toIndexConverter) {
		Contracts.assertNotNull( toIndexConverter, "toIndexConverter" );
		this.dslToIndexConverter = toIndexConverter;
	}

	public void projectionConverter(FromIndexFieldValueConverter<? super F, ?> fromIndexConverter) {
		Contracts.assertNotNull( fromIndexConverter, "fromIndexConverter" );
		this.projectionFromIndexConverter = fromIndexConverter;
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
	 * @return The user-configured converter for this field definition.
	 * @see org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTypedContext#dslConverter(ToIndexFieldValueConverter)
	 */
	public UserIndexFieldConverter<F> createUserIndexFieldConverter() {
		checkAccessorCreated();
		return new UserIndexFieldConverter<>(
				dslToIndexConverter,
				projectionFromIndexConverter
		);
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
