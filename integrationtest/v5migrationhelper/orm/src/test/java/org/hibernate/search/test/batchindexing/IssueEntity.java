/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.batchindexing;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

/**
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
@Entity
@Indexed
public class IssueEntity {

	@Id
	String jiraCode;

	@Field
	String jiraDescription;

}
