/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.showcase.library.model;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class Borrowal extends AbstractEntity<Integer> {

	@Id
	@GeneratedValue
	private Integer id;

	@ManyToOne(optional = false)
	private Account account;

	@ManyToOne(optional = false)
	private DocumentCopy<?> copy;

	@Basic(optional = false)
	private BorrowalType type;

	public Borrowal() {
	}

	public Borrowal(Account account, DocumentCopy<?> copy, BorrowalType type) {
		this.account = account;
		this.copy = copy;
		this.type = type;
	}

	@Override
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Account getAccount() {
		return account;
	}

	public void setAccount(Account account) {
		this.account = account;
	}

	public DocumentCopy<?> getCopy() {
		return copy;
	}

	public void setCopy(DocumentCopy<?> copy) {
		this.copy = copy;
	}

	public BorrowalType getType() {
		return type;
	}

	public void setType(BorrowalType type) {
		this.type = type;
	}

	@Override
	protected String getDescriptionForToString() {
		return "account=" + getAccount() + ",document=" + getCopy();
	}
}
