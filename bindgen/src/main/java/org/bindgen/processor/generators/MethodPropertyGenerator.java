package org.bindgen.processor.generators;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ExecutableType;

import joist.sourcegen.GClass;
import joist.sourcegen.GField;
import joist.sourcegen.GMethod;
import joist.util.Inflector;

import org.bindgen.ContainerBinding;
import org.bindgen.processor.util.BoundProperty;
import org.bindgen.processor.util.Util;

public class MethodPropertyGenerator implements PropertyGenerator {

	private final GClass outerClass;
	private final ExecutableElement method;
	private final String methodName;
	private final BoundProperty property;
	private GClass innerClass;

	public MethodPropertyGenerator(GClass outerClass, ExecutableElement method) {
		this.outerClass = outerClass;
		this.method = method;
		this.methodName = this.method.getSimpleName().toString();
		this.property = new BoundProperty(this.method, this.method.getReturnType(), this.guessPropertyNameOrNull());
	}

	@Override
	public boolean isCallable() {
		return false;
	}

	public boolean shouldGenerate() {
		if (this.property.shouldSkip() || this.methodThrowsExceptions() || this.methodHasParameters()) {
			return false;
		}
		return true;
	}

	public void generate() {
		this.addOuterClassGet();
		this.addOuterClassBindingField();
		this.addInnerClass();
		this.addInnerClassGetName();
		this.addInnerClassParent();
		this.addInnerClassGet();
		this.addInnerClassGetWithRoot();
		this.addInnerClassSet();
		this.addInnerClassSetWithRoot();
		this.addInnerClassGetContainedTypeIfNeeded();
	}

	private void addOuterClassGet() {
		GMethod fieldGet = this.outerClass.getMethod(this.property.getName() + "()");

		fieldGet.setAccess(Util.getAccess(this.method));

		fieldGet.returnType(this.property.getBindingClassFieldDeclaration());
		fieldGet.body.line("if (this.{} == null) {", this.property.getName());
		fieldGet.body.line("    this.{} = new {}();", this.property.getName(), this.property.getBindingRootClassInstantiation());
		fieldGet.body.line("}");
		fieldGet.body.line("return this.{};", this.property.getName());
		if (this.property.doesOuterGetNeedSuppressWarnings()) {
			fieldGet.addAnnotation("@SuppressWarnings(\"unchecked\")");
		}
	}

	private void addOuterClassBindingField() {
		GField f = this.outerClass.getField(this.property.getName()).type(this.property.getBindingClassFieldDeclaration());
		if (this.property.isRawType()) {
			f.addAnnotation("@SuppressWarnings(\"unchecked\")");
		}
	}

	private void addInnerClass() {
		this.innerClass = this.outerClass.getInnerClass(this.property.getInnerClassDeclaration()).notStatic();

		this.innerClass.setAccess(Util.getAccess(this.method));

		this.innerClass.baseClassName(this.property.getInnerClassSuperClass());
		if (this.property.doesInnerClassNeedSuppressWarnings()) {
			this.innerClass.addAnnotation("@SuppressWarnings(\"unchecked\")");
		}
		if (this.property.isForGenericTypeParameter() || this.property.isArray()) {
			this.innerClass.getMethod("getType").returnType("Class<?>").body.line("return null;");
		} else if (!this.property.shouldGenerateBindingClassForType()) {
			// since no binding class will be generated for the return type of this method we may not inherit getType() in MyBinding class (if, for example, MyBinding extends GenericObjectBindingPath) and so we have to implement it ouselves
			this.innerClass.getMethod("getType").returnType("Class<?>").body.line("return {}.class;", this.property.getSetType());
		}
	}

	private void addInnerClassGetName() {
		GMethod getName = this.innerClass.getMethod("getName").returnType(String.class).addAnnotation("@Override");
		getName.body.line("return \"{}\";", this.property.getName());
	}

	private void addInnerClassParent() {
		GMethod getParent = this.innerClass.getMethod("getParentBinding").returnType("Binding<?>").addAnnotation("@Override");
		getParent.body.line("return {}.this;", this.outerClass.getSimpleClassNameWithoutGeneric());
	}

	private void addInnerClassGet() {
		GMethod get = this.innerClass.getMethod("get");
		get.returnType(this.property.getSetType()).addAnnotation("@Override");
		get.body.line("return {}{}.this.get().{}();",//
			this.property.getCastForReturnIfNeeded(),
			this.outerClass.getSimpleClassNameWithoutGeneric(),
			this.methodName);
		if (this.property.doesInnerGetNeedSuppressWarnings()) {
			get.addAnnotation("@SuppressWarnings(\"unchecked\")");
		}
	}

	private void addInnerClassGetWithRoot() {
		GMethod getWithRoot = this.innerClass.getMethod("getWithRoot");
		getWithRoot.argument("R", "root").returnType(this.property.getSetType()).addAnnotation("@Override");
		getWithRoot.body.line("return {}{}.this.getWithRoot(root).{}();",//
			this.property.getCastForReturnIfNeeded(),
			this.outerClass.getSimpleClassNameWithoutGeneric(),
			this.methodName);
		if (this.property.doesInnerGetNeedSuppressWarnings()) {
			getWithRoot.addAnnotation("@SuppressWarnings(\"unchecked\")");
		}
	}

	private void addInnerClassSet() {
		GMethod set = this.innerClass.getMethod("set({} {})", this.property.getSetType(), this.property.getName()); // .addAnnotation("@Override");
		if (!this.hasSetter()) {
			set.body.line("throw new RuntimeException(this.getName() + \" is read only\");");
			return;
		}
		set.body.line("{}.this.get().{}({});",//
			this.outerClass.getSimpleClassNameWithoutGeneric(),
			this.getSetterName(),
			this.property.getName());
	}

	private void addInnerClassSetWithRoot() {
		GMethod setWithRoot = this.innerClass.getMethod("setWithRoot(R root, {} {})", this.property.getSetType(), this.property.getName()); // .addAnnotation("@Override");
		if (!this.hasSetter()) {
			setWithRoot.body.line("throw new RuntimeException(this.getName() + \" is read only\");");
			return;
		}
		setWithRoot.body.line("{}.this.getWithRoot(root).{}({});",//
			this.outerClass.getSimpleClassNameWithoutGeneric(),
			this.getSetterName(),
			this.property.getName());
	}

	private void addInnerClassGetContainedTypeIfNeeded() {
		if (this.property.isForListOrSet() && !this.property.matchesTypeParameterOfParent()) {
			this.innerClass.implementsInterface(ContainerBinding.class);
			GMethod getContainedType = this.innerClass.getMethod("getContainedType").returnType("Class<?>").addAnnotation("@Override");
			getContainedType.body.line("return {};", this.property.getContainedType());
		}
	}

	private boolean hasSetter() {
		String setterName = this.getSetterName();
		for (Element other : this.method.getEnclosingElement().getEnclosedElements()) {
			if (other.getSimpleName().toString().equals(setterName) && other.getModifiers().contains(Modifier.PUBLIC)) {
				ExecutableElement e = (ExecutableElement) other;
				return e.getParameters().size() == 1 && e.getThrownTypes().size() == 0; // only true if no throws
			}
		}
		return false;
	}

	private String getSetterName() {
		return "set" + this.methodName.substring(this.getPrefix().length());
	}

	private String getPrefix() {
		for (String possible : new String[] { "get", "to", "has", "is" }) {
			if (this.methodName.startsWith(possible)) {
				return possible;
			}
		}
		return null;
	}

	private String guessPropertyNameOrNull() {
		String propertyName = null;
		for (String possible : new String[] { "get", "to", "has", "is" }) {
			if (this.methodName.startsWith(possible)
				&& this.methodName.length() > possible.length() + 1
				&& this.methodName.substring(possible.length(), possible.length() + 1).matches("[A-Z]")) {
				propertyName = Inflector.uncapitalize(this.methodName.substring(possible.length()));
				break;
			}
		}
		if (Util.isJavaKeyword(propertyName) || "get".equals(propertyName)) {
			propertyName = this.methodName;
		}
		return propertyName;
	}

	private boolean methodThrowsExceptions() {
		return ((ExecutableType) this.method.asType()).getThrownTypes().size() > 0;
	}

	private boolean methodHasParameters() {
		return ((ExecutableType) this.method.asType()).getParameterTypes().size() > 0;
	}

	public TypeElement getPropertyTypeElement() {
		return this.property.getElement();
	}

	public String getPropertyName() {
		return this.property.getName();
	}

	@Override
	public String toString() {
		return this.method.toString();
	}
}