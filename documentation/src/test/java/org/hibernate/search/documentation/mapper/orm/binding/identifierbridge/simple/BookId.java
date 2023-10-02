/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.identifierbridge.simple;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Embeddable;

@Embeddable
public class BookId implements Serializable {

	private Long publisherId;

	private Long publisherSpecificBookId;

	protected BookId() {
		// For Hibernate ORM
	}

	public BookId(long publisherId, long publisherSpecificBookId) {
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
		BookId bookId = (BookId) o;
		return publisherId.equals( bookId.publisherId )
				&& publisherSpecificBookId.equals( bookId.publisherSpecificBookId );
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
