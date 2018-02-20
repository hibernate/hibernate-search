/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model;

import org.hibernate.search.engine.backend.document.model.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.ObjectFieldStorage;

/**
 * @author Guillaume Smet
 */
public interface LuceneIndexSchemaElement extends IndexSchemaElement {

	@Override
	default LuceneIndexSchemaObjectField objectField(String relativeName) {
		return objectField( relativeName, ObjectFieldStorage.DEFAULT );
	}

	@Override
	LuceneIndexSchemaObjectField objectField(String relativeName, ObjectFieldStorage storage);
}
