/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.path.id;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.DocumentId;

/**
 * @author Davide D'Alto
 */
@Entity
class DocumentEntity {

	@Id
	@GeneratedValue
	long id;

	@DocumentId
	String documentId;

	@OneToOne
	@ContainedIn
	EntityC c;

	public DocumentEntity() {
	}

	public DocumentEntity(String documentId) {
		this.documentId = documentId;
	}

}
