/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.document.model.dsl.dynamic;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.SortNatural;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.PropertyBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyBinding;

@Entity
@Indexed
public class Book {

	@Id
	private Integer id;

	@ElementCollection
	@SortNatural
	@PropertyBinding(binder = @PropertyBinderRef(type = UserMetadataBinder.class))
	private Map<String, String> userMetadata = new TreeMap<>();

	@ElementCollection
	@SortNatural
	@PropertyBinding(binder = @PropertyBinderRef(type = MultiTypeUserMetadataBinder.class))
	private Map<String, Serializable> multiTypeUserMetadata = new TreeMap<>();

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Map<String, String> getUserMetadata() {
		return userMetadata;
	}

	public Map<String, Serializable> getMultiTypeUserMetadata() {
		return multiTypeUserMetadata;
	}
}
