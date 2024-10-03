package com.padlocks;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class State {
    private final Map<String, Variable> storedVariables = new HashMap<>();
    private final Set<String> storedImports = new HashSet<>();
    private final Map<String, Class<?>> compiledClasses = new HashMap<>();
    private final Map<String, Method> compiledMethods = new HashMap<>();
    private final Map<String, String> classSources = new HashMap<>();


    // Variable Management
    public void addStoredVariable(String name, Variable variable) {
        storedVariables.put(name, variable);
    }

    public Variable getStoredVariable(String name) {
        return storedVariables.get(name);
    }

    public Map<String, Variable> getStoredVariables() {
        return storedVariables;
    }

    // Import Management
    public void addStoredImport(String input) {
        storedImports.add(input);
    }

    public Set<String> getStoredImports() {
        return storedImports;
    }

    // Compiled Class Management
    public void addCompiledClass(String name, Class<?> clazz, String sourceCode) {
        compiledClasses.put(name, clazz);
        classSources.put(name, sourceCode);
    }

    public Class<?> getCompiledClass(String name) {
        return compiledClasses.get(name);
    }

    public Map<String, Class<?>> getCompiledClasses() {
        return compiledClasses;
    }

    // Compiled Method Management
    public void addCompiledMethod(String name, Method method) {
        compiledMethods.put(name, method);
    }

    public Method getCompiledMethod(String name) {
        return compiledMethods.get(name);
    }

    public Map<String, Method> getCompiledMethods() {
        return compiledMethods;
    }

    // Source Code Management
    public void setClassSource(String className, String sourceCode) {
        classSources.put(className, sourceCode);
    }

    public String getClassSource(String className) {
        return classSources.get(className);
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();

        if (!storedImports.isEmpty()) {
            string.append("\n\nStored imports: \n");
            for (String importStatement : storedImports) {
                string.append(importStatement).append("\n");
            }
        }

        if (!storedVariables.isEmpty()) {
            string.append("\n\nStored variables: \n");
            for (Map.Entry<String, Variable> entry : storedVariables.entrySet()) {
                string.append(entry.getKey()).append(" = ").append(entry.getValue().getValue()).append("\n");
            }
        }

        if (!compiledClasses.isEmpty()) {
            string.append("\n\nCompiled classes: \n");
            for (Map.Entry<String, Class<?>> entry : compiledClasses.entrySet()) {
                string.append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
            }
        }

        if (!compiledMethods.isEmpty()) {
            string.append("\n\nCompiled methods: \n");
            for (Map.Entry<String, Method> entry : compiledMethods.entrySet()) {
                string.append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
            }
        }

        string.append("\n");

        return string.toString();
    }
}
