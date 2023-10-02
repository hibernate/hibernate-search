/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.backend.elasticsearch.type.asnative;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.NonStandardField;

// tag::include[]
@Entity
@Indexed
public class CompanyServer {

	@Id
	@GeneratedValue
	private Integer id;

	@NonStandardField( // <1>
			valueBinder = @ValueBinderRef(type = IpAddressValueBinder.class) // <2>
	)
	private String ipAddress;

	// Getters and setters
	// ...

	// tag::getters-setters[]
	public Integer getId() {
		return id;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	// end::getters-setters[]
}
// end::include[]
