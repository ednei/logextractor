package extractors;

import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import settings.Settings;

public class LogExtractor {

	private static Logger LOG = Logger.getLogger(LogExtractor.class.getName());
	
	public static void extract() {
		long startTime = System.currentTimeMillis();
		
		//DbUtils.getConn();
		// add the top of hierachy
		
		//TODO Review what's is that insert  
		//DbUtils.insertToStringRec("java.lang.Object", "@#@", "objaddr", 
		//		"java.lang.String", ToStringParser.PARSED_SUCCESSFUL);
		
		try {
			extractFromEclipseWorkspace();
		} catch (CoreException e) {
			e.printStackTrace();
		}
		
		//TODO Check what this method is doing		
		//processUnResolvedSubClasses(root, javaproject);
		
		//DbUtils.dumpTable("tostringdb");
		
		long timetaken = System.currentTimeMillis() - startTime;
		
		//DbUtils.closeConnection();
		
		LOG.info("TIME USED: " + timetaken/1000 + " seconds" );
	}
	
	public static void extractFromEclipseWorkspace() 
			throws CoreException{		
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		
		IProject project = root.getProject(Settings.getWorkspaceProjectName());

		project.open(null);

		IJavaProject javaproject = JavaCore.create(project);
				
		for(IPackageFragment pkgfrag: javaproject.getPackageFragments()) {
			//LOG.info("opening package " +pkgfrag.getElementName());
			for (ICompilationUnit cu : pkgfrag.getCompilationUnits() ) {
				String typename = pkgfrag.getElementName()
					+"."+cu.getElementName().replaceAll("\\.java", "");
				
				LOG.info("PROCESSING CLASS FILE: " + typename);
				IType tp = javaproject.findType(typename);
				processSourceFile(root, javaproject, tp);
			}
		}
	}
	
	
	public static void processSourceFile(IWorkspaceRoot root, IJavaProject javaproject, IType tp) 
			throws JavaModelException{
		
		ICompilationUnit cpunit = tp.getCompilationUnit();

		@SuppressWarnings("deprecation")
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setResolveBindings(true);
		parser.setSource(cpunit);
		CompilationUnit ast = (CompilationUnit) parser.createAST(null);
		MethodCallDetectorVisitor visitor = 
			new MethodCallDetectorVisitor(javaproject, ast,tp.getFullyQualifiedName());
		ast.accept(visitor);
		LogPrintStatementDetector logvisitor = 
			new LogPrintStatementDetector(javaproject, ast, tp.getFullyQualifiedName(),
				tp.getFullyQualifiedName(),"static");
		ast.accept(logvisitor);
	}
}
