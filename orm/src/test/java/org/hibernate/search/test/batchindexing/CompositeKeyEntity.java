/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.batchindexing;

import java.io.Serializable;

import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

import org.apache.commons.lang.StringUtils;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.bridge.TwoWayStringBridge;

/**
 * @author Chris Cranford
 */
@Entity
@Indexed
public class CompositeKeyEntity {

	@EmbeddedId
	@DocumentId
	@FieldBridge(impl = CompositeKeyEntity.Id.Bridge.class)
	private Id id;
	private String name;

	public Id getId() {
		return id;
	}

	public void setId(Id id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Embeddable
	public static class Id implements Serializable {
		private Integer ownerId;
		private Integer productId;

		Id() {

		}

		public Id(Integer ownerId, Integer productId) {
			this.ownerId = ownerId;
			this.productId = productId;
		}

		public Integer getOwnerId() {
			return ownerId;
		}

		public void setOwnerId(Integer ownerId) {
			this.ownerId = ownerId;
		}

		public Integer getProductId() {
			return productId;
		}

		public void setProductId(Integer productId) {
			this.productId = productId;
		}

		public static class Bridge implements TwoWayStringBridge {
			@Override
			public Object stringToObject(String stringValue) {
				String[] tokens = StringUtils.split(stringValue, ":");
				if ( tokens.length == 2 ) {
					return new Id( Integer.valueOf( tokens[0] ), Integer.valueOf( tokens[1] ) );
				}
				throw new IllegalArgumentException( "Unable to construct id from string." );
			}

			@Override
			public String objectToString(Object object) {
				if ( object instanceof Id ) {
					Id id = Id.class.cast(object);
					return id.getOwnerId() + ":" + id.getProductId();
				}
				throw new IllegalArgumentException( "Unable to construct string from id." );
			}
		}
	}
}
