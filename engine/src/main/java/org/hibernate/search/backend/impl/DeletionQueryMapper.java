/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl;

import org.apache.lucene.search.Query;
import org.hibernate.search.backend.DeletionQuery;
import org.hibernate.search.util.impl.ScopedAnalyzer;

/**
 * @author Martin Braun
 */
public interface DeletionQueryMapper {

	Query toLuceneQuery(DeletionQuery deletionQuery, ScopedAnalyzer analyzerForEntity);

	String[] toString(DeletionQuery deletionQuery);

	DeletionQuery fromString(String[] string);


	/*
	 * Why we use String arrays here: Why use Strings at all and not i.e. byte[] ?: As we want to support different
	 * versions of Queries later on and we don't want to write Serialization code over and over again in the
	 * Serialization module for all the different types we have one toString and fromString here. and we force to use
	 * Strings because i.e. using byte[] would suggest using Java Serialization for this process, but that would prevent
	 * us from changing our API internally in the future. Why not plain Strings?: But using plain Strings would leave us
	 * with yet another problem: We would have to encode all our different fields into a single string. For that we
	 * would need some magical chars to separate or we would need to use something like JSON/XML to pass the data
	 * consistently. This would be far too much overkill for us. By using String[] we force users (or API designers) to
	 * no use Java Serialization (well Serialization to BASE64 or another String representation is still possible, but
	 * nvm that) and we still have an _easy_ way of dealing with multiple fields in our different Query Types.
	 */

}
