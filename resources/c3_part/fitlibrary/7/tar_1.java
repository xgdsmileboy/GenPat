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
import fitlibrary.closure.LookupClosure;
import fitlibrary.global.PlugBoard;
import fitlibrary.parser.Parser;
import fitlibrary.parser.lookup.FieldParser;
import fitlibrary.parser.lookup.GetterParser;
import fitlibrary.parser.lookup.ResultParser;
import fitlibrary.traverse.Evaluator;
import fitlibrary.traverse.workflow.caller.ValidCall;
import fitlibrary.utility.ExtendedCamelCase;
import fitlibrary.utility.option.None;
import fitlibrary.utility.option.Option;
import fitlibrary.utility.option.Some;

public class NonGenericTypedObject implements TypedObject {
	protected final Object subject;
	protected final LookupClosure lookupClosure;
	protected final MethodTargetFactory methodTargetFactory;

	public interface MethodTargetFactory {
		CalledMethodTarget createCalledMethodTarget(Closure closure, Evaluator evaluator);
	}
	public NonGenericTypedObject(Object subject) {
		this(subject,PlugBoard.lookupClosure, new MethodTargetFactory(){
			@Override
			public CalledMethodTarget createCalledMethodTarget(Closure closure, Evaluator evaluator) {
				return new CalledMethodTarget(closure,evaluator);
			}});
	}
	public NonGenericTypedObject(Object subject, LookupClosure lookupClosure, MethodTargetFactory methodTargetFactory) { // Allows test injection
		this.subject = subject;
		this.lookupClosure = lookupClosure;
		this.methodTargetFactory = methodTargetFactory;
	}
	public Object getSubject() {
		return subject;
	}
	public Option<CalledMethodTarget> new_findSpecificMethod(String name, int argCount, Evaluator evaluator) {
		Option<Closure> methodClosureOption = new_findMethodClosure(name, argCount);
		if (methodClosureOption.isSome())
			return new Some<CalledMethodTarget>(methodTargetFactory.createCalledMethodTarget(methodClosureOption.get(), evaluator));
		return None.none();
	}
	private Option<Closure> new_findMethodClosure(String name, int argCount) {
		Closure methodClosure = lookupClosure.findMethodClosure(this,name, argCount);
		if (methodClosure == null)
			return None.none();
		return new Some<Closure>(methodClosure);
	}
	public CalledMethodTarget new_optionallyFindGetterOnTypedObject(String propertyName, Evaluator evaluator) {
		String getMethodName = ExtendedCamelCase.camel("get " + propertyName);
		Option<CalledMethodTarget> target = new_findSpecificMethod(getMethodName, 0, evaluator);
		if (target.isSome())
			return target.get();
		String isMethodName = ExtendedCamelCase.camel("is " + propertyName);
		target = new_findSpecificMethod(isMethodName, 0, evaluator);
		if (target.isSome())
			return target.get();
		return null;
	}
	public CalledMethodTarget optionallyFindMethodOnTypedObject(String name, int argCount, Evaluator evaluator, boolean includeSut) {
		Option<CalledMethodTarget> targetOption = new_findSpecificMethod(name,argCount,evaluator);
		if (targetOption.isSome())
			return targetOption.get();
		return null;
	}
	public void findMethodsFromPlainText(String textCall, List<ValidCall> results) {
		List<String> words = Arrays.asList(textCall.split(" "));
		Method[] methods = subject.getClass().getMethods();
		for (Method method : methods) {
			int argCount = method.getParameterTypes().length;
			if (method.getDeclaringClass() != Object.class
					&& !PlugBoard.lookupClosure.fitLibrarySystemMethod(method,argCount, subject)) {
				ValidCall.parseAction(words, method.getName(), argCount,results);
			}
		}
	}
	// Overridden in subclass
	protected TypedObject asTypedObject(Object sut) {
		return new NonGenericTypedObject(sut);
	}
	public Closure findPublicMethodClosureForTypedObject(String name, Class<?>[] argTypes) {
		return PlugBoard.lookupClosure.findPublicMethodClosure(this, name, argTypes);
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
	@Override
	public boolean equals(Object arg) {
		if (!(arg instanceof NonGenericTypedObject))
			return false;
		if (subject == null)
			return ((NonGenericTypedObject)arg).subject == null;
		return subject.equals(((NonGenericTypedObject)arg).subject);
	}
	@Override
	public int hashCode() {
		if (subject == null)
			return -123;
		return subject.hashCode();
	}
	@Override
	public boolean isNull() {
		return subject == null;
	}
}
