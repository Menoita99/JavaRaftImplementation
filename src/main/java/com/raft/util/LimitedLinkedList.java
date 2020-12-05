package com.raft.util;
import java.util.Collection;
import java.util.LinkedList;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LimitedLinkedList<E> extends LinkedList<E> {

	private static final long serialVersionUID = 1L;

	private final int maxSize;
	
	public LimitedLinkedList(int maxSize) {
		this.maxSize = maxSize;
	}
	
	
	public LimitedLinkedList(Collection<E> collection, int maxSize) {
		this.maxSize = maxSize;
		this.addAll(collection);
	}
	
	
	public LimitedLinkedList(Collection<E> collection) {
		this.maxSize = collection.size();
		this.addAll(collection);
	}
	
	
	@Override
	public boolean add(E e) {
		if(size()>=maxSize)
			removeFirst();
		return super.add(e);
	}
	
	
	@Override
	public boolean addAll(Collection<? extends E> c) {
		boolean output = super.addAll(c);
		while(size()>maxSize)
			removeFirst();
		return output;
	}
	
	public boolean isFull() {
		return size() == maxSize;
	}
}
