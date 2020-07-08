/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.bridge.identifierbridge.simple;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.IdentifierBridgeRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

// tag::include[]
@Entity
@Indexed
public class Book {

	@EmbeddedId
	@DocumentId( // <1>
			identifierBridge = @IdentifierBridgeRef(type = BookIdBridge.class) // <2>
	)
	private BookId id = new BookId();

	private String title;

	// Getters and setters
	// ...

	// tag::getters-setters[]
	public BookId getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
// end::getters-setters[]
}
// end::include[]
