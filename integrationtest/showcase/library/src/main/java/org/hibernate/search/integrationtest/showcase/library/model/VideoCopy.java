/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.model;

import jakarta.persistence.Entity;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;

/**
 * A concrete copy of a video document.
 *
 * @see DocumentCopy
 */
@Entity
public class VideoCopy extends DocumentCopy<Video> {

	@GenericField
	private VideoMedium medium;

	public VideoMedium getMedium() {
		return medium;
	}

	public void setMedium(VideoMedium medium) {
		this.medium = medium;
	}
}
