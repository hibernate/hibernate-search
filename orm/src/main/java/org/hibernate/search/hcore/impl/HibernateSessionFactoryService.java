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
package org.hibernate.search.hcore.impl;

import org.hibernate.SessionFactory;

import org.hibernate.search.engine.service.spi.Service;
import org.hibernate.search.engine.service.spi.Startable;

/**
 * A Hibernate Search service which allows to request a Hibernate {@code SessionFactory} during bootstrapping.
 *
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
public interface HibernateSessionFactoryService extends Service, Startable {
	SessionFactory getSessionFactory();
}
