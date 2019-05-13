/*
 * Copyright (c) 2006 Rick Mugridge, www.RimuResearch.com
 * Released under the terms of the GNU General Public License version 2 or later.
 * Written: 21/10/2006
 */

package fitlibrary.typed;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import fitlibrary.closure.CalledMethodTarget;
import fitlibrary.closure.Closure;
import fitlibrary.exception.method.MissingMethodException;
import fitlibrary.global.PlugBoard;
import fitlibrary.parser.Parser;
import fitlibrary.parser.lookup.FieldParser;
import fitlibrary.parser.lookup.GetterParser;
import fitlibrary.parser.lookup.ResultParser;
import fitlibrary.traverse.DomainAdapter;
import fitlibrary.traverse.Evaluator;
import fitlibrary.traverse.workflow.caller.ValidCall;
import fitlibrary.utility.ExtendedCamelCase;

public class NonGenericTypedObject implements TypedObject {
	protected Object subject;

	public NonGenericTypedObject(Object subject) {
		this.subject = subject;
	}
	public Object getSubject() {
		return subject;
	}
	public CalledMethodTarget findSpecificMethodOrPropertyGetter(String name,
			int argCount, Evaluator evaluator, List<String> signatures) throws Exception {
		CalledMethodTarget result = optionallyFindMethodOnTypedObject(name,
				argCount, evaluator, true);
		if (result != null)
			return result;
		try {
			if (argCount == 0)
				return findGetterOnTypedObject(name, evaluator);
		} catch (MissingMethodException e) {
			// Provide a more general error message
		}
		throw new MissingMethodException(signatures, PlugBoard.lookupTarget
				.identifiedClassesInSUTChain(subject));
	}
	private List<String> signatures(String... signature) {
		return Arrays.asList(signature);
	}
	public CalledMethodTarget findGetterOnTypedObject(String propertyName,
			Evaluator evaluator) {
		if (evaluator.getRuntimeContext() == null)
			throw new NullPointerException("runtime is null");
		CalledMethodTarget target = optionallyFindGetterOnTypedObject(
				propertyName, evaluator);
		if (target != null)
			return target;
		throw new MissingMethodException(signatures("public ResultType "
				+ ExtendedCamelCase.camel("get " + propertyName) + "() { }",
				"public ResultType "
						+ ExtendedCamelCase.camel("is " + propertyName)
						+ "() { }"), PlugBoard.lookupTarget
				.identifiedClassesInSUTChain(subject));
	}
	public CalledMethodTarget optionallyFindGetterOnTypedObject(
			String propertyName, Evaluator evaluator) {
		String getMethodName = ExtendedCamelCase.camel("get " + propertyName);
		CalledMethodTarget target = optionallyFindMethodOnTypedObject(
				getMethodName, 0, evaluator, true);
		if (target == null) {
			String isMethodName = ExtendedCamelCase.camel("is " + propertyName);
			target = optionallyFindMethodOnTypedObject(isMethodName, 0,
					evaluator, true);
		}
		return target;
	}
	public CalledMethodTarget optionallyFindMethodOnTypedObject(String name,
			int argCount, Evaluator evaluator, boolean includeSut) {
		Closure methodClosure = findMethodClosure(name, argCount, includeSut);
		if (methodClosure == null)
			return null;
		return new CalledMethodTarget(methodClosure, evaluator);
	}
	public void findMethodsFromPlainText(String textCall,
			List<ValidCall> results) {
		if (subject == null)
			return;
		List<String> words = Arrays.asList(textCall.split(" "));
		Method[] methods = subject.getClass().getMethods();
		int size = results.size();
		for (Method method : methods) {
			int argCount = method.getParameterTypes().length;
			if (method.getDeclaringClass() != Object.class
					&& !PlugBoard.lookupClosure.fitLibrarySystemMethod(method,
							argCount, subject)) {
				ValidCall.parseAction(words, method.getName(), argCount,
						results);
			}
		}
		if (results.size() == size && subject instanceof DomainAdapter) {
			DomainAdapter domainAdapter = (DomainAdapter) subject;
			Object nestedSystemUnderTest = domainAdapter.getSystemUnderTest();
			if (nestedSystemUnderTest != null)
				asTypedObject(nestedSystemUnderTest).findMethodsFromPlainText(
						textCall, results);
		}

	}
	public Closure findMethodClosure(String name, int argCount,
			boolean includeSut) {
		if (subject == null)
			return null;
		Closure methodClosure = PlugBoard.lookupClosure.findMethodClosure(this,
				name, argCount);
		if (methodClosure == null && subject instanceof Evaluator) {
			Evaluator evaluator = (Evaluator) subject;
			Object sut = evaluator.getSystemUnderTest();
			if (sut != null && (includeSut || sut instanceof DomainAdapter))
				methodClosure = evaluator.getTypedSystemUnderTest()
						.findMethodClosure(name, argCount, includeSut);
		}
		if (methodClosure == null && subject instanceof DomainAdapter) {
			DomainAdapter domainAdapter = (DomainAdapter) subject;
			Object nestedSystemUnderTest = domainAdapter.getSystemUnderTest();
			if (nestedSystemUnderTest != null
					&& (includeSut || nestedSystemUnderTest instanceof DomainAdapter))
				return asTypedObject(nestedSystemUnderTest).findMethodClosure(
						name, argCount, includeSut);
		}
		return methodClosure;
	}
	protected TypedObject asTypedObject(Object sut) {
		return new NonGenericTypedObject(sut);
	}
	public Closure findPublicMethodClosureForTypedObject(String name,
			Class<?>[] argTypes) {
		return PlugBoard.lookupClosure.findPublicMethodClosure(this, name,
				argTypes);
	}
	public Closure findMethodForTypedObject(String name, int argCount) {
		if (subject == null)
			return null;
		Closure chosenMethod = PlugBoard.lookupClosure.findMethodClosure(this,
				name, argCount);
		if (chosenMethod == null && subject instanceof DomainAdapter) {
			DomainAdapter domainAdapter = (DomainAdapter) subject;
			chosenMethod = asTypedObject(domainAdapter.getSystemUnderTest())
					.findMethodForTypedObject(name, argCount);
		}
		if (chosenMethod == null && subject instanceof Evaluator) {
			Evaluator evaluator = (Evaluator) subject;
			chosenMethod = asTypedObject(evaluator.getNextOuterContext())
					.findMethodForTypedObject(name, argCount);
		}
		return chosenMethod;
	}
	@Override
	public String toString() {
		return "NonGenericTypedObject[" + subject + "]";
	}
	public Class<?> getClassType() {
		return subject.getClass();
	}
	public ResultParser resultParser(Evaluator evaluator, Method method) {
		Typed resultTyped = resultTyped(method);
		return new GetterParser(getTyped().on(evaluator, resultTyped, true),
				method); // This doesn't handle String result case
	}
	public ResultParser resultParser(Evaluator evaluator, Field field) {
		Typed resultTyped = resultTyped(field);
		return new FieldParser(getTyped().on(evaluator, resultTyped, true),
				field); // This doesn't handle String result case
	}
	public ResultParser resultParser(Evaluator evaluator, Method method,
			Class<?> actualResultType) {
		Typed resultTyped = new NonGenericTyped(actualResultType, true);
		return new GetterParser(getTyped().on(evaluator, resultTyped, true),
				method);
	}
	public ResultParser resultParser(Evaluator evaluator, Field field,
			Class<?> actualResultType) {
		Typed resultTyped = new NonGenericTyped(actualResultType, true);
		return new FieldParser(getTyped().on(evaluator, resultTyped, true),
				field);
	}
	protected Typed resultTyped(Method method) {
		return new NonGenericTyped(method.getReturnType(), true);
	}
	protected Typed resultTyped(Field field) {
		return new NonGenericTyped(field.getType(), true);
	}
	public Parser[] parameterParsers(Evaluator evaluator, Method method) {
		Class<?>[] types = method.getParameterTypes();
		Parser[] parameterParsers = new Parser[types.length];
		for (int i = 0; i < types.length; i++) {
			Typed parameterTyped = parameterTyped(method, i);
			parameterParsers[i] = getTyped().on(evaluator, parameterTyped,
					false);
		}
		return parameterParsers;
	}
	public Typed getTyped() {
		return new NonGenericTyped(subject.getClass());
	}
	protected Typed parameterTyped(Method method, int parameterNo) {
		return new NonGenericTyped(method.getParameterTypes()[parameterNo],
				true);
	}
	public TypedObject asReturnTypedObject(Object object, Method method) {
		return new NonGenericTypedObject(object);
	}
	public TypedObject asReturnTypedObject(Object object, Field field) {
		return new NonGenericTypedObject(object);
	}
	public Evaluator traverse(Evaluator evaluator) {
		return getTyped().parser(evaluator).traverse(this);
	}
}
