/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.engine.numeric;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;

/**
 * @author Gustavo Fernandes
 */
class PinPoint {

	@DocumentId
	@Field(name = "numeric_id")
	@NumericField(forField = "numeric_id")
	private int id;

	@Field(store = Store.YES)
	private Integer stars;

	@AssociationInverseSide(inversePath = @ObjectPath(@PropertyValue(propertyName = "pinPoints")))
	private Location location;

	public PinPoint(int id, int stars, Location location) {
		this.id = id;
		this.stars = stars;
		this.location = location;
	}

	public PinPoint() {
	}

	public void setLocation(Location location) {
		this.location = location;
	}
}
