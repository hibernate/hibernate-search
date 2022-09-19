/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.spatial.genericfield;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

//tag::include[]
@Entity
@Indexed
public class Author {

	@Id
	@GeneratedValue
	private Integer id;

	private String name;

	@Embedded
	@GenericField // <3>
	private MyCoordinates placeOfBirth;

	public Author() {
	}

	// Getters and setters
	// ...

	//tag::getters-setters[]
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public MyCoordinates getPlaceOfBirth() {
		return placeOfBirth;
	}

	public void setPlaceOfBirth(MyCoordinates placeOfBirth) {
		this.placeOfBirth = placeOfBirth;
	}
	//end::getters-setters[]
}
//end::include[]
