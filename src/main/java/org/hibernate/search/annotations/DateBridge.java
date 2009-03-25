//$Id$
package org.hibernate.search.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.annotations.Resolution;

/**
 * Defines the temporal resolution of a given field
 * Date are stored as String in GMT
 *
 * @author Emmanuel Bernard
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( {ElementType.FIELD, ElementType.METHOD} )
@Documented
//TODO allow pattern like yyyyMMdd?
//TODO allow base timezone?
public @interface DateBridge {
	Resolution resolution();
}
