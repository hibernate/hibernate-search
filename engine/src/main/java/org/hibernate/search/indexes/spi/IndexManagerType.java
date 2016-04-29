/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.spi;


/**
 * Since Hibernate Search supports different types of indexing and query technologies,
 * such as embedding Apache Lucene or remote via Elasticsearch, the type of
 * such storage is represented by an instance implementing this interface.
 *
 * Instances of implementations of this interface could be used as keys in a Map,
 * so make sure to implement appropriate equals and hashCode functions.
 *
 * The purpose is that some components will have to adapt their output depending
 * on the target technology being used, so they might need to know the type.
 * We refrain from using Enums as that would not be extensible, and avoid using
 * the class type of an IndexManager to allow creating multiple custom
 * implementations for the same type of technology.
 *
 * @author Sanne Grinovero
 * @since 5.6
 */
public interface IndexManagerType {

}
