package br.unb.cic.reach.model;

import java.util.HashSet;
import java.util.Set;

import soot.SootClass;

public class ReachClass {
	private final String className;
	private final boolean isActivity;
	private final boolean isMainActivity;

	private final Set<ReachMethod> methods = new HashSet<>();

	public ReachClass(SootClass clazz, boolean isActivity, boolean isMainActivity) {
		this.className = clazz.getName();
		this.isActivity = isActivity;
		this.isMainActivity = isMainActivity;
	}

	public void addMethod(ReachMethod method) {
		if (method != null) {
			methods.add(method);
		}
	}

	public void removeMethod(ReachMethod method) {
		methods.remove(method);
	}

	public Set<ReachMethod> getMethods() {
		return methods;
	}

	public String getClassName() {
		return className;
	}

	public boolean isActivity() {
		return isActivity;
	}

	public boolean isMainActivity() {
		return isMainActivity;
	}

}
