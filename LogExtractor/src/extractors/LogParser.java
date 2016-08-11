package extractors;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;

public interface LogParser {
	public void processSourceFile(IJavaProject javaproject, IType tp);
}
