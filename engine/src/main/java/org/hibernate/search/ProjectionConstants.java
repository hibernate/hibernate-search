/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search;

/**
 * Defined projection constants.
 *
 * @author Emmanuel Bernard
 */
public interface ProjectionConstants {
	/**
	 * Represents the Hibernate entity returned in a search.
	 */
	String THIS = "__HSearch_This";

	/**
	 * The Lucene document returned by a search.
	 */
	String DOCUMENT = "__HSearch_Document";

	/**
	 * The legacy document's score from a search.
	 */
	String SCORE = "__HSearch_Score";

	/**
	 * Object id property
	 */
	String ID = "__HSearch_id";

	/**
	 * Lucene Document id.
	 * <p/>
	 * Expert: Lucene document id can change overtime between 2 different IndexReader opening.
	 *
	 * @experimental If you use this constant/feature, please speak up in the forum
	 */
	String DOCUMENT_ID = "__HSearch_DocumentId";

	/**
	 * Lucene {@link org.apache.lucene.search.Explanation} object describing the score computation for
	 * the matching object/document
	 * This feature is relatively expensive, do not use unless you return a limited
	 * amount of objects (using pagination)
	 * To retrieve explanation of a single result, consider retrieving {@link #DOCUMENT_ID}
	 * and using fullTextQuery.explain(int)
	 */
	String EXPLANATION = "__HSearch_Explanation";

	/**
	 * Represents the Hibernate entity class returned in a search. In contrast to the other constants this constant
	 * represents an actual field value of the underlying Lucene document and hence can directly be used in queries.
	 */
	String OBJECT_CLASS = "_hibernate_class";

	/**
	 * Represents the distance between an entity and the center of the search radius in case of a spatial query
	 */
	String SPATIAL_DISTANCE = "_HSearch_SpatialDistance";
}
