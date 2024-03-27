/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.showcase.library.model;

import jakarta.persistence.Entity;

/**
 * A video document.
 *
 * @see Document
 */
@Entity
public class Video extends Document<VideoCopy> {

	public Video() {
	}

	public Video(int id, String title, String author, String summary, String tags) {
		super( id, title, author, summary, tags );
	}
}
