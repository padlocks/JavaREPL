package com.padlocks;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class Compiler {
    private final State state;

    public Compiler(State state) {
        this.state = state;
    }

    public void compile(String fileName, String code) throws Exception {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

		// Create the directory if it doesn't exist
		File dir = new File("./tmp/");
		if (!dir.exists()) {
			dir.mkdirs();
		}

		// Write the code to a file
		File sourceFile = new File(dir, fileName);
		try (Writer writer = new FileWriter(sourceFile)) {
			writer.write(code);
		}

		// Collect all Java files in the tmp directory for compilation
		File[] allJavaFiles = dir.listFiles((d, name) -> name.endsWith(".java"));

		// Compile all the Java files
		Iterable<? extends JavaFileObject> fileObjects = fileManager.getJavaFileObjects(allJavaFiles);
		boolean compilationSuccess = compiler.getTask(null, fileManager, null, Arrays.asList("-d", "./tmp/"), null, fileObjects).call();
		if (!compilationSuccess) {
			throw new RuntimeException("Compilation failed.");
		}
	}


	public Object compileAndExecute(String className, String methodName, String code) throws Exception {
		compile(className + ".java", code);
		loadCompiledClass(className, code);

		// Execute the method
		Method method = state.getCompiledClass(className).getMethod(methodName);
		return method.invoke(null);
	}

	public void loadCompiledClass(String className, String sourceCode) throws Exception {
		// Create a new class loader with the compiled class directory
		URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{new File("./tmp/").toURI().toURL()});
		
		// Load the class using the updated class loader
		Class<?> compiledClass = Class.forName(className, true, classLoader);
		state.addCompiledClass(className, compiledClass, sourceCode);
		
		// Store any methods from the class into compiledMethods
		for (Method method : compiledClass.getDeclaredMethods()) {
			state.addCompiledMethod(className + "." + method.getName(), method);
		}
	}

	public void deleteCompiledFiles() {
		// Delete ./tmp directory
		File dir = new File("./tmp/");
		if (dir.exists()) {
			File[] files = dir.listFiles();
			if (files != null) {
				for (File file : files) {
					file.delete();
				}
			}
			dir.delete();
		}
	}
}
