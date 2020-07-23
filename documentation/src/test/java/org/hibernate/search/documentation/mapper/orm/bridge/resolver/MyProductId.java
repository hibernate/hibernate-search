/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.bridge.resolver;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Embeddable;

@Embeddable
public class MyProductId implements Serializable {

	private String producerId;

	private String producerProductId;

	protected MyProductId() {
		// For Hibernate ORM
	}

	public MyProductId(String producerId, String producerProductId) {
		this.producerId = producerId;
		this.producerProductId = producerProductId;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		MyProductId productId = (MyProductId) o;
		return producerId.equals( productId.producerId ) &&
				producerProductId.equals( productId.producerProductId );
	}

	@Override
	public int hashCode() {
		return Objects.hash( producerId, producerProductId );
	}

	public String getProducerId() {
		return producerId;
	}

	public void setProducerId(String producerId) {
		this.producerId = producerId;
	}

	public String getProducerProductId() {
		return producerProductId;
	}

	public void setProducerProductId(String producerProductId) {
		this.producerProductId = producerProductId;
	}
}
