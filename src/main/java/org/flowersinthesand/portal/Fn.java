package org.flowersinthesand.portal;

public abstract class Fn {

	public static interface Callback {
		void call() throws Throwable;
	}

	public static interface Callback1<A> {
		void call(A arg1) throws Throwable;
	}

	public static interface Callback2<A, B> {
		void call(A arg1, B arg2) throws Throwable;
	}

	public static interface Feedback<R> {
		R apply() throws Throwable;
	}

	public static interface Feedback1<R, A> {
		R apply(A arg1) throws Throwable;
	}

	public static interface Feedback2<R, A, B> {
		R apply(A arg1, B arg2) throws Throwable;
	}

}
