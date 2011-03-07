/* 
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

package org.hibernate.search.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * While extracting values from the index might be faster than extracting
 * them from a Database, it might still involve costly IO operations.
 * Depending on the type of performed queries, it's often needed to extract
 * at least the entity type. Caches which aren't needed won't be used, but used
 * caches might take a significant amount of memory.
 * 
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( {ElementType.TYPE} )
@Documented
public @interface CacheFromIndex {
	
	/**
	 * @return Returns a {@code FieldCache} enum type indicating what kind of caching we should use for index stored metadata. Defaults to {@code FieldCache.CLASS}.
	 */
	FieldCacheType[] value() default { FieldCacheType.CLASS };
	
}
