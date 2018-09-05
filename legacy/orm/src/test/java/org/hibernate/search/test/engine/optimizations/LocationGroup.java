/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.engine.optimizations;

import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Proxy;
import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.Field;

/**
 * Related to test case of HSEARCH-782; indexed properties are defined
 * via a programmatic configuration.
 *
 * @author Adam Harris
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
@Entity
@Proxy(lazy = false)
@Table(name = "location_group")
//not indexed
public class LocationGroup {

	@Id()
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long groupId;

	@Field
	@Column(length = 255)
	private String name;

	@ContainedIn
	@OneToMany(mappedBy = "locationGroup", cascade = { CascadeType.ALL }, fetch = FetchType.LAZY)
	Collection<Location> locations = new ArrayList<Location>();

	public LocationGroup() {
	}

	public LocationGroup(String name) {
		this.name = name;
	}

	public Long getGroupId() {
		return groupId;
	}

	public void setGroupId(Long groupId) {
		this.groupId = groupId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Collection<Location> getLocations() {
		return locations;
	}

	public void setLocations(Collection<Location> locations) {
		this.locations = locations;
	}

}
