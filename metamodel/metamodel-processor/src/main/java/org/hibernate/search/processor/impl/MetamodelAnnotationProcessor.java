package org.hibernate.search.processor.impl;

import javax.annotation.processing.RoundEnvironment;

public interface MetamodelAnnotationProcessor {

	void process(RoundEnvironment roundEnv);

}
