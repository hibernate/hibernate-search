/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.identifierbridge.compatible;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Embeddable;

@Embeddable
public class BookOrMagazineId implements Serializable {

	private Long publisherId;

	private Long publisherSpecificBookId;

	protected BookOrMagazineId() {
		// For Hibernate ORM
	}

	public BookOrMagazineId(long publisherId, long publisherSpecificBookId) {
		this.publisherId = publisherId;
		this.publisherSpecificBookId = publisherSpecificBookId;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		BookOrMagazineId bookOrMagazineId = (BookOrMagazineId) o;
		return publisherId.equals( bookOrMagazineId.publisherId )
				&& publisherSpecificBookId.equals( bookOrMagazineId.publisherSpecificBookId );
	}

	@Override
	public int hashCode() {
		return Objects.hash( publisherId, publisherSpecificBookId );
	}

	public Long getPublisherId() {
		return publisherId;
	}

	protected void setPublisherId(Long publisherId) {
		this.publisherId = publisherId;
	}

	public Long getPublisherSpecificBookId() {
		return publisherSpecificBookId;
	}

	protected void setPublisherSpecificBookId(Long publisherSpecificBookId) {
		this.publisherSpecificBookId = publisherSpecificBookId;
	}
}
