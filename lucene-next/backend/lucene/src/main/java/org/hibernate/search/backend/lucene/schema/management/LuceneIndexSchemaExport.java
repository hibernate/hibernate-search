/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.schema.management;

import org.hibernate.search.engine.common.schema.management.SchemaExport;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * Extended version of an {@link SchemaExport} that exposes any Lucene-specific methods.
 */
@Incubating
public interface LuceneIndexSchemaExport extends SchemaExport {
}
