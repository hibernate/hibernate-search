/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.jakarta.batch.massindexing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

/**
 * "Who Am I" is a poorly-formed entity. It has multiple ID-like fields, which make it difficult to identify the real
 * identifier.
 *
 * @author Mincong Huang
 */
@Entity
@Indexed
public class WhoAmI {

	@Id
	private String customId;

	@FullTextField
	private String id;

	@GenericField
	@Column(name = "`uid`")
	private String uid;

	public WhoAmI() {
	}

	public WhoAmI(String customId, String id, String uid) {
		this.customId = customId;
		this.id = id;
		this.uid = uid;
	}

	public String getCustomId() {
		return customId;
	}

	public void setCustomId(String customId) {
		this.customId = customId;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}
}
