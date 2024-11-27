/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.projection.dsl;

import java.util.Collection;
import java.util.Map;

import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.util.common.annotation.Incubating;

import org.apache.lucene.document.Document;

/**
 * Represents an indexed document tree.
 * <p>
 * With Hibernate Search, indexed objects that use {@link ObjectStructure#NESTED nested object structure}
 * will be indexed as child documents. This document tree recreates the tree structure of such indexed nested documents.
 */
@Incubating
public interface DocumentTree {
	/**
	 * @return The document that represents the current node in the tree.
	 */
	Document document();

	/**
	 * @return A map representing nested document nodes relative to the current node.
	 * The map key represents the name of a nested field within the current node,
	 * while the map value contains tree nodes representing the documents of that nested field.
	 */
	Map<String, Collection<DocumentTree>> nested();
}
