/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.batchindexing;

import jakarta.persistence.Entity;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

/**
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
@Entity
@Indexed
public class ExtendedIssueEntity extends IssueEntity {

	@Field
	Long id;

}
