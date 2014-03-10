/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
/**
 * Classes offering a service infrastructure for Search.
 *
 * Entry point is the {@code ServiceManager} which allows to retrieve and release services. Services can be provided
 * programmatically or discovered via Java's {@link java.util.ServiceLoader} mechanism.
 *
 * In order to be a service an interface must extend the {@code Service} interface. Optionally a service can also
 * implement {@code Startable} and/or {@code Stoppable} in order to get live cycle callbacks.
 *
 * @author Hardy Ferentschik
 */
package org.hibernate.search.engine.service.spi;
