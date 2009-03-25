//$Id$
package org.hibernate.search.test.embedded;

import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.DocumentId;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "`Order`")
public class Order {
	@Id
	@GeneratedValue
	@DocumentId
	private Integer id;
	@Field(index= Index.UN_TOKENIZED)
	private String orderNumber;
	

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getOrderNumber() {
		return orderNumber;
	}

	public void setOrderNumber(String orderNumber) {
		this.orderNumber = orderNumber;
	}
}
