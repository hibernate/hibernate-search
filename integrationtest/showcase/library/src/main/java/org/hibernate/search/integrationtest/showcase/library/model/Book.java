/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.model;

import javax.persistence.Basic;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Field;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FunctionBridgeBeanReference;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.integrationtest.showcase.library.bridge.ISBNBridge;
import org.hibernate.search.integrationtest.showcase.library.usertype.ISBNUserType;

/**
 * A mainly textual document.
 *
 * @see Document
 */
@Entity
@Indexed(index = Book.INDEX)
public class Book extends Document<BookCopy> {

	static final String INDEX = "Book";

	@Basic
	@Type(type = ISBNUserType.NAME)
	@Field(functionBridge = @FunctionBridgeBeanReference(type = ISBNBridge.class))
	private ISBN isbn;

	public ISBN getIsbn() {
		return isbn;
	}

	public void setIsbn(ISBN isbn) {
		this.isbn = isbn;
	}
}
