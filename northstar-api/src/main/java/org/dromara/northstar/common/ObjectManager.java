package org.dromara.northstar.common;

import java.util.List;

import org.dromara.northstar.common.model.Identifier;

public interface ObjectManager<T> {

	void add(T t);
	
	void remove(Identifier id);
	
	T get(Identifier id);
	
	boolean contains(Identifier id);
	
	List<T> findAll();
}
