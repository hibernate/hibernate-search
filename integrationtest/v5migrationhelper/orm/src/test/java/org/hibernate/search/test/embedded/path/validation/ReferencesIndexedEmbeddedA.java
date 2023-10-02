/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.test.embedded.path.validation;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;

/**
 * @author zkurey
 */
@Entity
@Indexed
public class ReferencesIndexedEmbeddedA {

	@Id
	@GeneratedValue
	public int id;

	@ManyToOne
	@IndexedEmbedded
	@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
	public A a;

}
