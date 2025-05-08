package ca.yorku.nemo.main;

import java.util.Objects;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class MethodNodeForOutput {
	CompilationUnit cu;
	MethodDeclaration md;
	String filePath = "";
	
	int startLine;
	
	public MethodNodeForOutput(CompilationUnit cu, MethodDeclaration md, String filePath) {
		this.cu = cu;
		this.md = md;
		this.filePath = filePath;
		startLine = cu.getLineNumber(md.getStartPosition());
	}
	
	@Override
	public String toString() {
		String sb = filePath + "," +md.getName() + ":" + cu.getLineNumber(md.getStartPosition());
		return sb;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(filePath, startLine);
	}
	
	@Override
	public boolean equals(Object node) {
		
		if (node instanceof MethodNodeForOutput) {
			MethodNodeForOutput toCompare = (MethodNodeForOutput) node;
			return (((MethodNodeForOutput) node).filePath.equals(toCompare.filePath) && 
					((MethodNodeForOutput) node).startLine == toCompare.startLine);
		}
		return false;
	}
}
