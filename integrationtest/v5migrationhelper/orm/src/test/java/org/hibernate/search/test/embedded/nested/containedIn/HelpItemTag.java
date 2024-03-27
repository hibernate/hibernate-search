/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.test.embedded.nested.containedIn;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;

import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class HelpItemTag {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Version
	private Long version;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "helpItem", nullable = false)
	private HelpItem helpItem;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "tag", nullable = false)
	@IndexedEmbedded
	private Tag tag;

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

	public HelpItem getHelpItem() {
		return helpItem;
	}

	public void setHelpItem(HelpItem helpItem) {
		this.helpItem = helpItem;
	}

	public Tag getTag() {
		return tag;
	}

	public void setTag(Tag tag) {
		this.tag = tag;
	}
}
