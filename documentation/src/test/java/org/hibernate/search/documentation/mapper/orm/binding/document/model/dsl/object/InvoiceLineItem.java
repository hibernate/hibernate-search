/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.document.model.dsl.object;

import java.math.BigDecimal;

import jakarta.persistence.Embeddable;

@Embeddable
public class InvoiceLineItem {

	private InvoiceLineItemCategory category;

	private BigDecimal amount;

	protected InvoiceLineItem() {
		// For Hibernate ORM
	}

	public InvoiceLineItem(InvoiceLineItemCategory category, BigDecimal amount) {
		this.category = category;
		this.amount = amount;
	}

	public InvoiceLineItemCategory getCategory() {
		return category;
	}

	public void setCategory(InvoiceLineItemCategory category) {
		this.category = category;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}
}
