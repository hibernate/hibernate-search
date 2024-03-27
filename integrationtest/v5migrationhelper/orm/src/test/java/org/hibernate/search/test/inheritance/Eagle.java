/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.inheritance;

import jakarta.persistence.Entity;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

/**
 * @author Hardy Ferentschik
 */
@Entity
@Indexed
public class Eagle extends Bird {
	private WingType wingType;

	@Field(analyze = Analyze.NO, store = Store.YES)
	public WingType getWingType() {
		return wingType;
	}

	public void setWingType(WingType wingType) {
		this.wingType = wingType;
	}

	public enum WingType {
		BROAD,
		LONG
	}
}
