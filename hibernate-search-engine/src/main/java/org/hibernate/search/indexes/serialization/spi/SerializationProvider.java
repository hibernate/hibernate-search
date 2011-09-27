/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.indexes.serialization.spi;

/**
 * Serialization provider
 *
 * Providers are encouraged to offer a backward and forward compatible protocol.
 *
 * //FIXME should these two bytes be processed by Hibernate Search and not the protocol implementation
 * ie we would pass the result of this?
 *
 * Before the actual serialized flux, two bytes are reserved:
 * - majorVersion
 * - minorVersion
 *
 * A major version increase implies an incompatible protocol change.
 * Messages of a majorVersion > current version should be refused.
 *
 * A minor version increase implies a compatible protocol change.
 * Messages of a minorVersion > current version are parsed but new
 * operation will be ignored or rejected. Question: only ignored?
 *
 * If message's major version is < current version, the
 * implementation is strongly encouraged to parse and process them.
 * It is mandatory if only message's minor version is < current version.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public interface SerializationProvider {
	Serializer getSerializer();
	Deserializer getDeserializer();
}
