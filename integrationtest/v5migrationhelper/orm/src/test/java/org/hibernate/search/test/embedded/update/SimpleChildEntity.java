/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.embedded.update;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;

@Entity
@Indexed
public class SimpleChildEntity {

	@Id
	@GeneratedValue
	private Long id;

	@OneToOne(mappedBy = "child", optional = false)
	private SimpleParentEntity parent;

	protected SimpleChildEntity() {
	}

	public SimpleChildEntity(SimpleParentEntity parent) {
		this.parent = parent;
	}

	@Field(analyze = Analyze.NO)
	@IndexingDependency(derivedFrom = @ObjectPath({
			@PropertyValue(propertyName = "parent"),
			@PropertyValue(propertyName = "name")
	}))
	public String getParentName() {
		return parent.getName();
	}
}
