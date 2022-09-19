/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
