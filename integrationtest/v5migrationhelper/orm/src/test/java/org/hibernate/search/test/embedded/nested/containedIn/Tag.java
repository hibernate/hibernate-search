/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.test.embedded.nested.containedIn;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;

import org.hibernate.search.annotations.Field;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Tag {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Version
	private Long version;

	@Column(nullable = false, length = 50)
	@Field
	private String name;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "tag")
	private List<HelpItemTag> helpItems;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<HelpItemTag> getHelpItems() {
		if ( helpItems == null ) {
			helpItems = new ArrayList<HelpItemTag>();
		}
		return helpItems;
	}

	public void setHelpItems(List<HelpItemTag> helpItems) {
		this.helpItems = helpItems;
	}

}
