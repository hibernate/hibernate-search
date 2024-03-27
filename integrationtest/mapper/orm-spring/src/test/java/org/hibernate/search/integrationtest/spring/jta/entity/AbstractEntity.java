/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.spring.jta.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.annotations.NaturalId;

@MappedSuperclass
public abstract class AbstractEntity {

	@NaturalId(mutable = true)
	@Column(length = 36)
	private String magicKey_;

	/**
	 * Sets the magic key for the instance
	 */
	public AbstractEntity() {
		magicKey_ = UUID.randomUUID().toString();
	}

	/**
	 * @return the magicKey
	 */
	public String getMagicKey() {
		return magicKey_;
	}

	/**
	 * @param magicKey the magicKey to set
	 */
	public void setMagicKey(String magicKey) {
		magicKey_ = magicKey;
	}
}
