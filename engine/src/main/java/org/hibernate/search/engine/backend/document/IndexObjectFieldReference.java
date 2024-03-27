/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.document;


/**
 * A reference to an "object" field of an indexed document,
 * allowing to add new values to this field for a given document.
 *
 * @see DocumentElement#addObject(IndexObjectFieldReference)
 * @see DocumentElement#addNullObject(IndexObjectFieldReference)
 */
public interface IndexObjectFieldReference {

}
