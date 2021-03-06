package org.bindgen.processor;

import static org.junit.Assert.*;

import org.junit.Test;

public class InnerClassTest extends AbstractBindgenTestCase {

	@Test
	public void testGenerateBindingsForInnerClasses() throws Exception {
		ClassLoader loader = this.compile("org/bindgen/processor/inner/SomeClass.java");

		Class<?> actualClass = loader.loadClass("org.bindgen.processor.inner.SomeClass$InnerClass");
		assertNotNull(actualClass);

		Class<?> bindingClass = loader.loadClass("org.bindgen.processor.inner.someClass.InnerClassBindingPath");
		assertNotNull(bindingClass);
		assertMethodDeclared(bindingClass, "x");
		assertMethodDeclared(bindingClass, "squared");
	}

	@Test
	public void testGenerateBindingsForNastyInnerClasses() throws Exception {
		ClassLoader loader = this.compile("org/bindgen/processor/inner/nastyClass.java");

		Class<?> actualClass = loader.loadClass("org.bindgen.processor.inner.nastyClass$InnerClass");
		assertNotNull(actualClass);

		Class<?> bindingClass = loader.loadClass("org.bindgen.processor.inner.bindgen_nastyClass.InnerClassBindingPath");
		assertNotNull(bindingClass);
		assertMethodDeclared(bindingClass, "x");
	}
}
