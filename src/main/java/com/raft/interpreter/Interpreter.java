package com.raft.interpreter;


import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Interpreter {

	private Binding binding = new Binding();
	private GroovyShell shell = new GroovyShell(binding);
	
	public Object execute(String command) {
		System.out.println("Executing -> "+command);
		return shell.evaluate(command);
	}
}
