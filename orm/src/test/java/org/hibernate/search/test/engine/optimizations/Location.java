/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.engine.optimizations;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.annotations.Proxy;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * Related to test case of HSEARCH-782; indexed properties are defined
 * via a programmatic configuration.
 *
 * @author Adam Harris
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
@Entity
@Proxy(lazy = false)
@Table(name = "location")
@Indexed
public class Location {

	@Id()
	@GeneratedValue(strategy = GenerationType.AUTO)
	@DocumentId
	private Long locationId;

	@Column(length = 255)
	@Field
	private String name;

	@ManyToOne(fetch = FetchType.LAZY, targetEntity = LocationGroup.class)
	@JoinColumn(name = "location_group_id")
	@LazyToOne(LazyToOneOption.PROXY)
	@IndexedEmbedded(depth = 1)
	private LocationGroup locationGroup;

	public Location() {
	}

	public Location(String name) {
		this.name = name;
	}

	public Long getLocationId() {
		return locationId;
	}

	public void setLocationId(Long locationId) {
		this.locationId = locationId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public LocationGroup getLocationGroup() {
		return locationGroup;
	}

	public void setLocationGroup(LocationGroup locationGroup) {
		this.locationGroup = locationGroup;
	}

}
