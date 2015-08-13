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
 * Serialize {@code LuceneWork} instances.
 *
 * Needed for clustering where we need to serialize the work to remote nodes.
 * <p>
 * <b>Note</b>:<br>
 * Implementations need to be threadsafe.
 * </p>
 *
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt; (C) 2011 Red Hat Inc.
 */
public interface LuceneWorkSerializer {

	/**
	 * Convert a List of LuceneWork into a byte[].
	 * @param works the list of {@link LuceneWork}
	 * @return the list of {@link LuceneWork} as byte[]
	 */
	byte[] toSerializedModel(List<LuceneWork> works);

	/**
	 * Convert a byte[] to a List of LuceneWork.
	 * @param data the byte array to convert
	 * @return the list of {@link LuceneWork}
	 */
	List<LuceneWork> toLuceneWorks(byte[] data);

	/**
	 * @return a short label of this implementation and optionally version
	 */
	String describeSerializer();

}
