/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.test.jpa.entities;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.genericjpa.annotations.InIndex;

/**
 * Created by Martin on 23.06.2015.
 */
@Indexed
@InIndex
public class NonJPAEntity {

	@DocumentId
	private String documentId;

	@Field
	private String someField;

	public String getDocumentId() {
		return documentId;
	}

	public NonJPAEntity setDocumentId(String documentId) {
		this.documentId = documentId;
		return this;
	}
}
