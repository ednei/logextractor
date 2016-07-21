package extractors.java.android;

import java.util.List;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;

import core.LogParser;
import core.StringExprExpandVisitor;
import data.ElementList;

public class RlogParser extends LogParser{

	public RlogParser(IJavaProject javaproj, CompilationUnit compilationunit, String compilationUnitName) {
		this(javaproj,compilationunit,compilationUnitName);
	}

	@Override
	public boolean canParseClass(String classname) {
		return "Rlog".equals(classname);
	}

	@Override
	public boolean visit(MethodInvocation node) {
		@SuppressWarnings("unchecked")
		List<ASTNode> args = node.arguments();
		ASTNode arg;
		String level = null; // log debug level
		String loggerMethodName = node.getName().toString();
		if (loggerMethodName.startsWith("is") 
				|| loggerMethodName.startsWith("set") 
				|| loggerMethodName.startsWith("get")) {
			return true;
		} else if (args.size() < 1) {
			LOG.warning("Log print statement has no arguments ::: ");
			LOG.warning("ERRPR LOG::"
					+ node
					+ " Line:"
					+ compilationunit
							.getLineNumber(node.getStartPosition())
					+ " in " + compilationUnitName + " LEVEL:"
					+ node.getName());
			return true;
		} else if (args.size() == 1) {
			if (node.getName().toString().equals("getLogger")) {
				return true;
			}
			level = node.getName().toString();
			arg = args.get(0);
		} else {
			
			if (logtype==LOGTYPE_LOG4J)  {
				level = node.getName().toString();
				arg = args.get(0);
				if (! arg.toString().startsWith("\"")) {
					LOG.info("skipping method " + node.getName() + "(" +arg.toString() +")");
				}
			} else if (logtype == LOGTYPE_JDKLOG){
				
				// first see if it is logThrow method
				int formatStrInd = 1;
				//LOG.info("method name=" + node.getName());
				if(node.getName().toString().equals("logThrow")) {
					LOG.finer("logThrow methods");
					formatStrInd = 2;
					//return true;
				}
				
				
				String levelexpr = args.get(0).toString();
				level = levelexpr.substring(levelexpr.lastIndexOf(".")+1 );
				
				// visit each format string arguments
				int line = this.compilationunit.getLineNumber(node
						.getStartPosition());
				arg = args.get(formatStrInd);
				
				ElementList resultlist = new ElementList(
						this.compilationUnitName, line);
				StringExprExpandVisitor visitor = new StringExprExpandVisitor(
						this.javaproject, this.compilationunit, resultlist);
				arg.accept(visitor);
				
				String regexpr = visitor.result.toRegExpr(false);
				@SuppressWarnings("unused")
				String namemap = visitor.result.getNameMapString();
				@SuppressWarnings("unused")
				String typemap = visitor.result.getTypeMapString();
				String constStr = visitor.result.getConstantString();
				
				/*if (LOG.isLoggable(Level.FINER)) {
				//LOG.finer( "result: " + visitor.result.toString() );
					LOG.finer("line " + line);
					LOG.finer("regexpr: " + regexpr);
					LOG.finer("namemap: " + namemap);
					LOG.finer("typemap: " + typemap);
					LOG.finer("constString: " + constStr);
				}*/
				
				if(args.size()>formatStrInd+1) {
					// do string expand on each arguments
					// first save the result name/type map, and regexpr
					
					LOG.info(args.get(formatStrInd+1).getClass().toString());
					if (args.get(formatStrInd+1) instanceof ArrayCreation){
						ArrayCreation tc = (ArrayCreation) args.get(formatStrInd+1);
						ArrayInitializer t = tc.getInitializer();
						for (int i=0; i<t.expressions().size(); i++ ) {
							Expression e = (Expression) t.expressions().get(i);
							arg = e;
							resultlist = new ElementList(
									this.compilationUnitName, line);
							visitor = new StringExprExpandVisitor(
									this.javaproject, this.compilationunit, resultlist);
							arg.accept(visitor);
							LOG.finer("VISITING ARG in Array " + i);
							//LOG.info( "result: " + visitor.result.toString() );
							String r = visitor.result.toRegExpr(false);
							regexpr = regexpr.replaceFirst("\\\\\\{"+ i +"[^\\}]*" +"\\\\\\}", r);
							r = visitor.result.getConstantString();
							constStr = constStr.replaceFirst("\\{"+ i +"[^\\}]*" +"\\}", r);
							namemap += visitor.result.getNameMapString();
							typemap += visitor.result.getTypeMapString();
							
						}
						
					} else {
						// List l = visitor.result.list;
						for (int i = formatStrInd + 1; i < args.size(); i++) {
							arg = args.get(i);
							resultlist = new ElementList(
									this.compilationUnitName, line);
							visitor = new StringExprExpandVisitor(
									this.javaproject, this.compilationunit,
									resultlist);
							arg.accept(visitor);
							LOG.finer("VISITING ARG" + i);
							// LOG.info( "result: " +
							// visitor.result.toString() );
							String r = visitor.result.toRegExpr(false);
							regexpr = regexpr
									.replaceFirst("\\\\\\{"
											+ (i - 1 - formatStrInd) +"[^\\}]*"
											+ "\\\\\\}", r);
							r = visitor.result.getConstantString();
							constStr = constStr.replaceFirst("\\{"
									+ (i - 1 - formatStrInd) +"[^\\}]*"+ "\\}", r);
							namemap += visitor.result.getNameMapString();
							typemap += visitor.result.getTypeMapString();

						}
					}
					
					regexpr=regexpr.replaceAll("\\\\\\{[0-9]+\\\\\\}", ".*");
					constStr=constStr.replaceAll("\\{[0-9]+\\}", "");
				}
				
				// insert into DB
				@SuppressWarnings("unused")
				String logid = this.compilationUnitName + "-" + line;
				/*
				int methodid = DbUtils.findMethodId(this.currentClass,
						this.currentMethod, "");
				DbUtils.insertLogEntry(logid, regexpr, namemap,
						typemap, constStr, 
						Utils.getLevel(level), inloop>0, methodid);
				LOG.info("LOG print regexpr: " + regexpr);
				*/
				return true;
			} else {
				throw new RuntimeException("Logtype not supported!!");
			}
		}
		
		processOneLoggerCall(node, arg, level);

	} else {
		String text = node.getName().toString();
		if (text.equals("message") ) {
			String level = "info";
			@SuppressWarnings("unchecked")
			List<ASTNode> args = node.arguments();
			ASTNode arg = args.get(0);
			processOneLoggerCall(node, arg, level);
		}
	}
		
	}

}
