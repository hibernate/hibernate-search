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

package org.hibernate.search.test.engine;

import org.hibernate.search.Environment;

/**
 * Verify functionality of Environment.ENABLE_DIRTY_CHECK
 *
 * @author Sanne Grinovero
 * @since 3.4
 */
public class DirtyChecksDisabledTest extends SkipIndexingWorkForUnaffectingChangesTest {

	@Override
	protected boolean isDirtyCheckEnabled() {
		return false;
	}

	@Override
	protected void configure(org.hibernate.cfg.Configuration configuration) {
		super.configure( configuration );
		cfg.setProperty( Environment.ENABLE_DIRTY_CHECK, " false" ); //intentional space in the value
	}
}
