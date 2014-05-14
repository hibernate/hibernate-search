/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
/**
 * Represents a canonical model for the data that ought to be serialized.
 * A JavaSerializationSerializationProvider convert List of LuceneWork into this canonical
 * model.
 *
 * Note that some objects are reused in the Serialization / Deserialization contract used by
 * other serializers but most are not and this code is mostly unused.
 *
 * Still it is a useful and typesafe references that we might want to keep around.
 *
 * @author Emmanuel Bernard
 */
package org.hibernate.search.indexes.serialization.javaserialization.impl;
