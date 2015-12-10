/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.impl;

import org.hibernate.search.backend.LuceneWork;


/**
 * @author Gunnar Morling
 */
public class DocumentIdHelper {

	private DocumentIdHelper() {
	}

	static String getDocumentId(LuceneWork work) {
		return work.getTenantId() == null ? work.getIdInString() : work.getTenantId() + "_" + work.getIdInString();
	}

	static String getEntityId(String documentId) {
		if ( documentId.contains( "_" ) ) {
			documentId = documentId.substring( documentId.indexOf( "_" ) + 1 );
		}

		return documentId;
	}
}
