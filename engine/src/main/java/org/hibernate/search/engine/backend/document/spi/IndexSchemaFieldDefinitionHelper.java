/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.spi;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.PassThroughFromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.PassThroughToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeConverterContext;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaBuildContext;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.impl.common.Contracts;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * A helper for backends, making it easier to return accessors before they are completely defined,
 * and creating the user-defined converter to apply in search queries.
 */
public final class IndexSchemaFieldDefinitionHelper<F> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final IndexSchemaBuildContext schemaContext;

	private final DeferredInitializationIndexFieldAccessor<F> deferredInitializationAccessor =
			new DeferredInitializationIndexFieldAccessor<>();

	private final Class<F> indexFieldType;

	private ToDocumentFieldValueConverter<?, ? extends F> dslToIndexConverter;
	private FromDocumentFieldValueConverter<? super F, ?> indexToProjectionConverter;

	private boolean accessorCreated = false;

	public IndexSchemaFieldDefinitionHelper(IndexSchemaBuildContext schemaContext, Class<F> indexFieldType) {
		this.schemaContext = schemaContext;
		this.indexFieldType = indexFieldType;
		this.dslToIndexConverter = null;
		this.indexToProjectionConverter = null;
	}

	public IndexSchemaBuildContext getSchemaContext() {
		return schemaContext;
	}

	public void dslConverter(ToDocumentFieldValueConverter<?, ? extends F> toIndexConverter) {
		Contracts.assertNotNull( toIndexConverter, "toIndexConverter" );
		this.dslToIndexConverter = toIndexConverter;
	}

	public void projectionConverter(FromDocumentFieldValueConverter<? super F, ?> fromIndexConverter) {
		Contracts.assertNotNull( fromIndexConverter, "fromIndexConverter" );
		this.indexToProjectionConverter = fromIndexConverter;
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
	 * @return The user-configured converter for this field definition, or a default converter if none was configured.
	 * @see IndexFieldTypeConverterContext#dslConverter(ToDocumentFieldValueConverter)
	 */
	public ToDocumentFieldValueConverter<?, ? extends F> createDslToIndexConverter() {
		checkAccessorCreated();
		return dslToIndexConverter == null ? new PassThroughToDocumentFieldValueConverter<>( indexFieldType )
				: dslToIndexConverter;
	}

	/**
	 * @return The user-configured converter for this field definition, or a default converter if none was configured.
	 * @see IndexFieldTypeConverterContext#projectionConverter(FromDocumentFieldValueConverter)
	 */
	public FromDocumentFieldValueConverter<? super F, ?> createIndexToProjectionConverter() {
		checkAccessorCreated();
		/*
		 * TODO HSEARCH-3257 when no projection converter is configured, create a projection converter that will throw an exception
		 * with an explicit message.
		 * Currently we create a pass-through converter because users have no way to bypass the converter.
		 */
		return indexToProjectionConverter == null ? new PassThroughFromDocumentFieldValueConverter<>( indexFieldType )
				: indexToProjectionConverter;
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
