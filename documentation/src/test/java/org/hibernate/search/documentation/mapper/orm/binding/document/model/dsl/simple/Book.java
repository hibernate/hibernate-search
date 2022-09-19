/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.binding.document.model.dsl.simple;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.search.documentation.testsupport.data.ISBN;
import org.hibernate.search.documentation.testsupport.data.ISBNAttributeConverter;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.PropertyBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyBinding;

@Entity
@Indexed
public class Book {

	@Id
	@GeneratedValue
	private Integer id;

	@Convert(converter = ISBNAttributeConverter.class)
	@PropertyBinding(binder = @PropertyBinderRef(type = ISBNBinder.class))
	private ISBN isbn;

	public Integer getId() {
		return id;
	}

	public ISBN getIsbn() {
		return isbn;
	}

	public void setIsbn(ISBN isbn) {
		this.isbn = isbn;
	}
}
