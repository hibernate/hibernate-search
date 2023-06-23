/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.binding.propertybridge.param.annotation;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OrderColumn;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

// tag::include[]
@Entity
@Indexed
public class Invoice {

	@Id
	@GeneratedValue
	private Integer id;

	@ElementCollection
	@OrderColumn
	@InvoiceLineItemsSummaryBinding( // <1>
			fieldName = "itemSummary"
	)
	private List<InvoiceLineItem> lineItems = new ArrayList<>();

	// Getters and setters
	// ...

	// tag::getters-setters[]
	public Integer getId() {
		return id;
	}

	public List<InvoiceLineItem> getLineItems() {
		return lineItems;
	}

	public void setLineItems(List<InvoiceLineItem> lineItems) {
		this.lineItems = lineItems;
	}
	// end::getters-setters[]
}
// end::include[]
