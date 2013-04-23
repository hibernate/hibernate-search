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
package org.hibernate.search.query.hibernate.impl;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.TimeoutManager;

/**
 * Interface defining a set of operations in order to load entities which matched a query. Depending on the type of
 * indexed entities and the type of query different strategies can be used.
 *
 *
 * @author Emmanuel Bernard
 */
public interface Loader {
	void init(
			Session session,
			SearchFactoryImplementor searchFactoryImplementor,
			ObjectsInitializer objectsInitializer,
			TimeoutManager timeoutManager);

	Object load(EntityInfo entityInfo);

	Object loadWithoutTiming(EntityInfo entityInfo);

	List load(EntityInfo... entityInfos);

	boolean isSizeSafe();
}
