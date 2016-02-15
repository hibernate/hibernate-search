/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.test.jpa.entities;

import org.hibernate.search.genericjpa.annotations.DtoField;
import org.hibernate.search.genericjpa.annotations.DtoFields;
import org.hibernate.search.genericjpa.annotations.DtoOverEntity;

/**
 * Created by Martin on 22.06.2015.
 */
@DtoOverEntity(entityClass = Place.class)
public class TestDto {

	@DtoFields({@DtoField(fieldName = "name"), @DtoField(fieldName = "id", profileName = "ID_PROFILE")})
	private Object field;

	public Object getField() {
		return field;
	}

	public void setField(Object field) {
		this.field = field;
	}
}
