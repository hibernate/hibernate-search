/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.identifiermapping.customtype;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.documentation.testsupport.data.ISBN;
import org.hibernate.search.documentation.testsupport.data.ISBNAttributeConverter;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.IdentifierBridgeRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

//tag::include[]
@Entity
@Indexed
public class Book {

	@Id
	@Convert(converter = ISBNAttributeConverter.class)
	@DocumentId(identifierBridge = @IdentifierBridgeRef(type = ISBNIdentifierBridge.class))
	private ISBN isbn;

	public Book() {
	}

	// Getters and setters
	// ...

	//tag::getters-setters[]
	public ISBN getIsbn() {
		return isbn;
	}

	public void setIsbn(ISBN isbn) {
		this.isbn = isbn;
	}
	//end::getters-setters[]
}
//end::include[]
