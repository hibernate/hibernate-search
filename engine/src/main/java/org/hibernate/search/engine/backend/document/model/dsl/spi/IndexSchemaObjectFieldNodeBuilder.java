/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.dsl.spi;

import org.hibernate.search.engine.backend.document.IndexObjectFieldAccessor;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;

public interface IndexSchemaObjectFieldNodeBuilder extends IndexSchemaObjectNodeBuilder {

	// FIXME remove this method
	default IndexObjectFieldAccessor createAccessor() {
		return getReference();
	}

	IndexObjectFieldReference getReference();

}
