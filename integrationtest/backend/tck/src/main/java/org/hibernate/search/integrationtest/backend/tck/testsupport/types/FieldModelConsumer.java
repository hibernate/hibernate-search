/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

public interface FieldModelConsumer<E, M> {

	void accept(FieldTypeDescriptor<?, ?> typeDescriptor, E expectations, M fieldModel);

}
