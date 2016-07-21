package extractors;

import java.util.logging.Logger;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;

import data.ElementList;

public abstract class LogParser {

	protected IJavaProject javaproject;
	protected CompilationUnit compilationunit;
	protected String compilationUnitName;
	protected static Logger log = Logger.getLogger(LogParser.class.getName());
	
	public LogParser(IJavaProject javaproj, CompilationUnit compilationunit, String compilationUnitName) {
		this.javaproject = javaproj;
		this.compilationunit = compilationunit;
		this.compilationUnitName = compilationUnitName;
	}
	
	protected void processOneLoggerCall(MethodInvocation node, ASTNode arg, String level) {	
		log.info("LOG print str: " + arg);
		if (arg instanceof Expression) {
			int line = this.compilationunit.getLineNumber(node
					.getStartPosition());
			ElementList resultlist = new ElementList(
					this.compilationUnitName, line);
			StringExprExpandVisitor visitor = new StringExprExpandVisitor(
					this.javaproject, this.compilationunit, resultlist);
			arg.accept(visitor);
			
			/*
			if (LOG.isLoggable(Level.FINER)) {
				LOG.finer( "result: " + visitor.result.toString() );
				LOG.finer("regexpr: " + visitor.result.toRegExpr(true));
				LOG.finer("namemap: " + visitor.result.getNameMapString());
				LOG.finer("typemap: " + visitor.result.getTypeMapString());
			}*/
			
			@SuppressWarnings("unused")
			Statement codeblock = (Statement) Utils.findBlockStatement(node);
			//int blockline = compilationunit.getLineNumber(codeblock
			//		.getStartPosition());
			/*
			int methodid = DbUtils.findMethodId(this.currentClass,
					this.currentMethod, "");
			if (LOG.isLoggable(Level.FINER)) {
				//LOG.finer("BLOCK_LINE:" + blockline);
				LOG.finer("LOG IN LOOP: " + inloop);
				LOG.finer("METHOD: " + this.currentMethod + " "+ methodid);
			}
			
			if (methodid == -1) {
				LOG.warning("FAILED TO FIND METHOD:: "
						+ this.currentClass + " " + this.currentMethod);
			}
			DbUtils.insertLogEntry(this.compilationUnitName + "-" + line,
					visitor.result, Utils.getLevel(level), inloop > 0,
					methodid);
			 */
		} else {
			log.warning("NODE is of type " + arg.getClass());
		}	
	}

	public abstract boolean canParseClass(String classname);

	public abstract boolean visit(MethodInvocation node);

}
