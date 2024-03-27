/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.document;


/**
 * A reference to an "object" field of an indexed document,
 * allowing to add new values to this field for a given document.
 *
 * @param <F> The indexed field value type.
 *
 * @see DocumentElement#addValue(IndexFieldReference, Object)
 */
@SuppressWarnings("unused") // The type parameter is used in methods accepting this interface as a parameter.
public interface IndexFieldReference<F> {

}
