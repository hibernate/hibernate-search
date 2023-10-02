/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.schema.management.impl;

import org.hibernate.search.engine.backend.schema.management.spi.IndexSchemaManager;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

public interface PojoSchemaManagementIndexedTypeContext {

	PojoRawTypeIdentifier<?> typeIdentifier();

	IndexSchemaManager schemaManager();

}
