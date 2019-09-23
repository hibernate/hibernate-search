/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.bridge.valuebridge.binder;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.documentation.testsupport.data.ISBN;
import org.hibernate.search.documentation.testsupport.data.ISBNAttributeConverter;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

// tag::include[]
@Entity
@Indexed
public class Book {

	@Id
	@GeneratedValue
	private Integer id;

	@Convert(converter = ISBNAttributeConverter.class)
	@KeywordField( // <1>
			valueBinder = @ValueBinderRef(type = ISBNValueBinder.class), // <2>
			sortable = Sortable.YES // <3>
	)
	private ISBN isbn;

	// Getters and setters
	// ...

	// tag::getters-setters[]
	public Integer getId() {
		return id;
	}

	public ISBN getIsbn() {
		return isbn;
	}

	public void setIsbn(ISBN isbn) {
		this.isbn = isbn;
	}
	// end::getters-setters[]
}
// end::include[]
