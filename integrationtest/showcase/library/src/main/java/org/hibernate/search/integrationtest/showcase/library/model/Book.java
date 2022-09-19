/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.model;

import jakarta.persistence.Basic;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;

import org.hibernate.search.integrationtest.showcase.library.attributeconverter.ISBNAttributeConverter;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.integrationtest.showcase.library.bridge.ISBNBridge;

/**
 * A mainly textual document.
 *
 * @see Document
 */
@Entity
public class Book extends Document<BookCopy> {

	@Basic
	@Convert(converter = ISBNAttributeConverter.class)
	@GenericField(valueBinder = @ValueBinderRef(type = ISBNBridge.Binder.class))
	private ISBN isbn;

	public Book() {
	}

	public Book(int id, String isbn, String title, String author, String summary, String tags) {
		super( id, title, author, summary, tags );
		this.isbn = new ISBN( isbn );
	}

	public ISBN getIsbn() {
		return isbn;
	}

	public void setIsbn(ISBN isbn) {
		this.isbn = isbn;
	}
}
