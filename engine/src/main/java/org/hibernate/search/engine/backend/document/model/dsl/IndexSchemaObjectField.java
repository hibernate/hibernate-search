/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.document.model.dsl;

import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;

/**
 * An object field in the index schema,
 * allowing the definition of child fields,
 * customization of some characteristics of this object field such as {@link #multiValued() multi-valued-ness},
 * and the retrieval of {@link #toReference() a field reference} to be used when indexing.
 *
 * @see IndexSchemaElement
 * @see IndexSchemaFieldOptionsStep
 * @see IndexSchemaFieldFinalStep
 */
public interface IndexSchemaObjectField
		extends IndexSchemaElement, IndexSchemaFieldOptionsStep<IndexSchemaObjectField, IndexObjectFieldReference> {

}
