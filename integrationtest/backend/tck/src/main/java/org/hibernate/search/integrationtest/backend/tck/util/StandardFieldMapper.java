/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.util;

import java.util.function.Consumer;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.StandardIndexSchemaFieldTypedContext;

@FunctionalInterface
public interface StandardFieldMapper<F, M> {

	default M map(IndexSchemaElement parent, String name) {
		return map( parent, name, ignored -> { } );
	}

	M map(IndexSchemaElement parent, String name, Consumer<StandardIndexSchemaFieldTypedContext<F>> additionalConfiguration);

}
