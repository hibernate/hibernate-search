/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
