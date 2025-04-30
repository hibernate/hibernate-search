/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.processor.model;

import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.GeoPointBinding;
import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.Latitude;
import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.Longitude;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Indexed
public class MyGeoPointBindingFieldEntity {

	@DocumentId
	private String id;

	@GeoPointBinding
	private MyCoordinates placeOfBirth;

	public static class MyCoordinates {

		@Latitude
		private Double latitude;

		@Longitude
		private Double longitude;
	}
}
