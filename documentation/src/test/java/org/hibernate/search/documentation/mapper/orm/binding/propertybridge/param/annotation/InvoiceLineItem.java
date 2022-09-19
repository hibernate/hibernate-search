/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.binding.propertybridge.param.annotation;

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
