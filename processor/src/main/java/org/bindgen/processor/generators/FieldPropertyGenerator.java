package org.bindgen.processor.generators;

import java.util.Collection;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

import joist.sourcegen.GClass;
import joist.sourcegen.GField;
import joist.sourcegen.GMethod;

import org.bindgen.ContainerBinding;
import org.bindgen.processor.util.BoundProperty;
import org.bindgen.processor.util.Util;

/**
 * Generates bindings for fields 
 */
public class FieldPropertyGenerator implements PropertyGenerator {

	private final GClass outerClass;
	private final Element field;
	private final BoundProperty property;
	private final boolean isFinal;
	private GClass innerClass;

	public FieldPropertyGenerator(GClass outerClass, Element field, Collection<String> accessorNames, Collection<String> otherNamesTaken)
		throws WrongGeneratorException {
		this.outerClass = outerClass;
		this.field = field;

		String propertyName = this.field.getSimpleName().toString(); // we already have a binding to the accessor
		if (accessorNames.contains(propertyName)) {
			throw new WrongGeneratorException();
		}
		while (otherNamesTaken.contains(propertyName)) {
			propertyName += "Field";
		}

		this.property = new BoundProperty(this.field, this.field.asType(), propertyName);
		if (this.property.shouldSkip()) {
			throw new WrongGeneratorException();
		}
		this.isFinal = this.field.getModifiers().contains(javax.lang.model.element.Modifier.FINAL);
	}

	@Override
	public boolean hasSubBindings() {
		return true;
	}

	public void generate() {
		this.addOuterClassGet();
		this.addOuterClassBindingField();
		this.addInnerClass();
		this.addInnerClassGetName();
		this.addInnerClassGetParent();
		this.addInnerClassGet();
		this.addInnerClassGetWithRoot();
		this.addInnerClassSet();
		this.addInnerClassSetWithRoot();
		this.addInnerClassGetContainedTypeIfNeeded();
		this.addInnerClassSerialVersionUID();
	}

	private void addOuterClassBindingField() {
		GField f = this.outerClass.getField(this.property.getName()).type(this.property.getBindingClassFieldDeclaration());
		if (this.property.isRawType()) {
			f.addAnnotation("@SuppressWarnings(\"unchecked\")");
		}
	}

	private void addOuterClassGet() {
		GMethod fieldGet = this.outerClass.getMethod(this.property.getName() + "()");
		fieldGet.setAccess(Util.getAccess(this.field));
		fieldGet.returnType(this.property.getBindingClassFieldDeclaration());
		fieldGet.body.line("if (this.{} == null) {", this.property.getName());
		fieldGet.body.line("    this.{} = new {}();", this.property.getName(), this.property.getBindingRootClassInstantiation());
		fieldGet.body.line("}");
		fieldGet.body.line("return this.{};", this.property.getName());
		if (this.property.doesOuterGetNeedSuppressWarnings()) {
			fieldGet.addAnnotation("@SuppressWarnings(\"unchecked\")");
		}
	}

	private void addInnerClass() {
		this.innerClass = this.outerClass.getInnerClass(this.property.getInnerClassDeclaration()).notStatic();
		this.innerClass.setAccess(Util.getAccess(this.field));
		this.innerClass.baseClassName(this.property.getInnerClassSuperClass());
		if (this.property.doesInnerClassNeedSuppressWarnings()) {
			this.innerClass.addAnnotation("@SuppressWarnings(\"unchecked\")");
		}
		if (this.property.isForGenericTypeParameter() || this.property.isArray()) {
			this.innerClass.getMethod("getType").returnType("Class<?>").body.line("return null;");
		} else if (!this.property.shouldGenerateBindingClassForType()) {
			// since no binding class will be generated for the type of this field we may not inherit getType() in MyBinding class (if, for example, MyBinding extends GenericObjectBindingPath) and so we have to implement it ouselves
			this.innerClass.getMethod("getType").returnType("Class<?>").body.line("return {}.class;", this.property.getReturnableType());
		}
	}

	private void addInnerClassGetName() {
		GMethod getName = this.innerClass.getMethod("getName").returnType(String.class).addAnnotation("@Override");
		getName.body.line("return \"{}\";", this.property.getName());
	}

	private void addInnerClassGetParent() {
		GMethod getParent = this.innerClass.getMethod("getParentBinding").returnType("Binding<?>").addAnnotation("@Override");
		getParent.body.line("return {}.this;", this.outerClass.getSimpleClassNameWithoutGeneric());
	}

	private void addInnerClassGet() {
		GMethod get = this.innerClass.getMethod("get").returnType(this.property.getSetType()).addAnnotation("@Override");
		get.body.line("return {}{}.this.get().{};",//
			this.property.getCastForReturnIfNeeded(),
			this.outerClass.getSimpleClassNameWithoutGeneric(),
			this.property.getName());
		if (this.property.doesInnerGetNeedSuppressWarnings()) {
			get.addAnnotation("@SuppressWarnings(\"unchecked\")");
		}
	}

	private void addInnerClassGetWithRoot() {
		GMethod getWithRoot = this.innerClass.getMethod("getWithRoot");
		getWithRoot.argument("R", "root").returnType(this.property.getSetType()).addAnnotation("@Override");
		getWithRoot.body.line("return {}{}.this.getWithRoot(root).{};",//
			this.property.getCastForReturnIfNeeded(),
			this.outerClass.getSimpleClassNameWithoutGeneric(),
			this.property.getName());
		if (this.property.doesInnerGetNeedSuppressWarnings()) {
			getWithRoot.addAnnotation("@SuppressWarnings(\"unchecked\")");
		}
	}

	private void addInnerClassSet() {
		GMethod set = this.innerClass.getMethod("set").argument(this.property.getSetType(), this.property.getName());
		set.addAnnotation("@Override");
		if (this.isFinal) {
			set.body.line("throw new RuntimeException(this.getName() + \" is read only\");");
			return;
		}
		set.body.line("{}.this.get().{} = {};",//
			this.outerClass.getSimpleClassNameWithoutGeneric(),
			this.property.getName(),
			this.property.getName());
	}

	private void addInnerClassSetWithRoot() {
		GMethod setWithRoot = this.innerClass.getMethod("setWithRoot(R root, {} {})", this.property.getSetType(), this.property.getName());
		setWithRoot.addAnnotation("@Override");
		if (this.isFinal) {
			setWithRoot.body.line("throw new RuntimeException(this.getName() + \" is read only\");");
			return;
		}
		setWithRoot.body.line(
			"{}.this.getWithRoot(root).{} = {};",
			this.outerClass.getSimpleClassNameWithoutGeneric(),
			this.property.getName(),
			this.property.getName());
	}

	private void addInnerClassGetContainedTypeIfNeeded() {
		if (this.property.isForListOrSet() && !this.property.matchesTypeParameterOfParent()) {
			this.innerClass.implementsInterface(ContainerBinding.class);
			GMethod getContainedType = this.innerClass.getMethod("getContainedType").returnType("Class<?>").addAnnotation("@Override");
			getContainedType.body.line("return {};", this.property.getContainedType());
		}
	}

	private void addInnerClassSerialVersionUID() {
		this.innerClass.getField("serialVersionUID").type("long").setStatic().setFinal().initialValue("1L");
	}

	public TypeElement getPropertyTypeElement() {
		return this.property.getElement();
	}

	public String getPropertyName() {
		return this.property.getName();
	}

	@Override
	public String toString() {
		return this.field.toString();
	}

	public static class Factory implements GeneratorFactory {
		private AccessorMethodGenerator.Factory accessorFactory;

		@Override
		public FieldPropertyGenerator newGenerator(GClass outerClass, Element possibleField, Collection<String> namesTaken) throws WrongGeneratorException {
			if (possibleField.getKind() != ElementKind.FIELD) {
				throw new WrongGeneratorException();
			}
			Collection<String> accessorNames = this.accessorFactory.getAccessorNames();
			return new FieldPropertyGenerator(outerClass, possibleField, accessorNames, namesTaken);
		}

		public Factory setAccessorFactory(AccessorMethodGenerator.Factory accessorFactory) {
			this.accessorFactory = accessorFactory;
			return this;
		}
	}
}
