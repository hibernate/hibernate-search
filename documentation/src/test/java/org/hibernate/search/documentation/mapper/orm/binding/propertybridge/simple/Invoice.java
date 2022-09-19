/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.binding.propertybridge.simple;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.PropertyBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyBinding;

// tag::include[]
@Entity
@Indexed
public class Invoice {

	@Id
	@GeneratedValue
	private Integer id;

	@ElementCollection
	@OrderColumn
	@PropertyBinding(binder = @PropertyBinderRef(type = InvoiceLineItemsSummaryBinder.class)) // <1>
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
