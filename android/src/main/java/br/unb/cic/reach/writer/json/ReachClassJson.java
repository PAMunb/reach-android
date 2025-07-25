package br.unb.cic.reach.writer.json;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import br.unb.cic.reach.model.ReachClass;

public class ReachClassJson {
	private String className;
	private boolean isActivity;
	private boolean isMainActivity;
	private Set<ReachMethodJson> methods = new HashSet<>();

	public ReachClassJson(ReachClass clazz) {
		this.className = clazz.getClassName();
		this.isActivity = clazz.isActivity();
		this.isMainActivity = clazz.isMainActivity();

		if (clazz.getMethods() != null) {
			methods = clazz.getMethods().stream().map(ReachMethodJson::new).collect(Collectors.toSet());
		}
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public boolean isActivity() {
		return isActivity;
	}

	public void setActivity(boolean isActivity) {
		this.isActivity = isActivity;
	}

	public boolean isMainActivity() {
		return isMainActivity;
	}

	public void setMainActivity(boolean isMainActivity) {
		this.isMainActivity = isMainActivity;
	}

	public Set<ReachMethodJson> getMethods() {
		return methods;
	}

	public void setMethods(Set<ReachMethodJson> methods) {
		this.methods = methods;
	}

}
