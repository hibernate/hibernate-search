/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
