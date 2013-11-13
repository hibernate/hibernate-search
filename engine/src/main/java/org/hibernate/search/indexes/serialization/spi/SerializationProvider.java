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
 * Provides access to a serializer and deserializer to send the necessary work load for remote backends over the
 * wire.
 * <p>
 * Note: Providers are encouraged to offer a backward and forward compatible protocol.
 * </p>
 * <p>
 * Before the actual serialized flux, two bytes are reserved:
 * <ul>
 * <li>majorVersion</li>
 * <li>minorVersion</li>
 * </ul>
 *
 * A major version increase implies an incompatible protocol change.
 * Messages of a {@code majorVersion > current version} should be refused.
 *
 * A minor version increase implies a compatible protocol change.
 * Messages of a {@code minorVersion > current version} are parsed, but new
 * operation will be ignored or rejected.
 *
 * If message's {@code major version is < current version}, then the
 * implementation is strongly encouraged to parse and process them.
 * It is mandatory if only message's {@code code minor version is < current version}.
 * </p>
 * <p>
 * Implementors are encouraged to implement a descriptive {@code toString()}
 * method for logging purposes.
 * </p>
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
// TODO should these two version bytes be processed by Hibernate Search and not the protocol implementation
// ie we would pass the result of this?
public interface SerializationProvider {
	Serializer getSerializer();

	Deserializer getDeserializer();
}
