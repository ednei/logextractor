package extractors.java.android;

import java.util.logging.Level;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodInvocation;

public interface LogClassParser {
	
	boolean isMethodInvocationOnLoggerClass(MethodInvocation node);

	Level getLevel();

	ASTNode getMessage();

}
