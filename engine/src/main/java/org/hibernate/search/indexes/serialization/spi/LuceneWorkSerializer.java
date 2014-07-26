/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.serialization.spi;

import java.util.List;

import org.hibernate.search.backend.LuceneWork;

/**
 * For clustering we need some way to serialize the {@code LuceneWork} instances.
 * to the other nodes.
 * Implementations need to be threadsafe.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public interface LuceneWorkSerializer {

	/**
	 * Convert a List of LuceneWork into a byte[]
	 */
	byte[] toSerializedModel(List<LuceneWork> works);

	/**
	 * Convert a byte[] to a List of LuceneWork
	 */
	List<LuceneWork> toLuceneWorks(byte[] data);

	/**
	 * @return a short label of this implementation and optionally version
	 */
	String describeSerializer();

}
