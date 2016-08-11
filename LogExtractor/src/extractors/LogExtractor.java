package extractors;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import data.DbUtils;
import extractors.java.android.AndroidLogParser;
import settings.Settings;

public class LogExtractor {

	private static Logger LOG = Logger.getLogger(LogExtractor.class.getName());
	
	public static void extract() {
		long startTime = System.currentTimeMillis();
		
		DbUtils.getConn();
		// add the top of hierachy
		
		//DbUtils.insertToStringRec("java.lang.Object", "@#@", "objaddr", 
		//		"java.lang.String", ToStringParser.PARSED_SUCCESSFUL);
		
		DbUtils.insertToStringRec("java.lang.Object", "(.*)", "objaddr", 
						"java.lang.String", ToStringParser.PARSED_SUCCESSFUL);
				
		
		try {
			extractFromEclipseWorkspace();
		} catch (CoreException e) {
			e.printStackTrace();
		}
		
		//DbUtils.dumpTable("tostringdb");
		
		long timetaken = System.currentTimeMillis() - startTime;
		
		DbUtils.closeConnection();
		
		LOG.info("TIME USED: " + timetaken/1000 + " seconds" );
	}
	
	public static void extractFromEclipseWorkspace() 
			throws CoreException{		
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		
		IProject project = root.getProject(Settings.getWorkspaceProjectName());

		project.open(null);

		IJavaProject javaproject = JavaCore.create(project);
		
		LogParser logParser = LogExtractor.getLogParserForFramework();
				
		for(IPackageFragment pkgfrag: javaproject.getPackageFragments()) {
			//LOG.info("opening package " +pkgfrag.getElementName());
			for (ICompilationUnit cu : pkgfrag.getCompilationUnits() ) {
				String typename = pkgfrag.getElementName()
					+"."+cu.getElementName().replaceAll("\\.java", "");
				LOG.info("PROCESSING CLASS FILE: " + typename);
				IType tp = javaproject.findType(typename);
				logParser.processSourceFile(javaproject, tp);
			}
		}
		//TODO Check what this method is doing		
		processUnResolvedSubClasses(root, javaproject);		
	}
	
	public static LogParser getLogParserForFramework() {
		return new AndroidLogParser();
	}
	
	//TODO move this has a high coupling with database implementation and parser implementation fix this
	static void processUnResolvedSubClasses(IWorkspaceRoot root, IJavaProject javaproject){
		try{
			Statement st = DbUtils.getConn().createStatement();
			ToStringParser parser = new ToStringParser(javaproject);
			ResultSet subclasses = st.executeQuery("select subclass from tostringsubclass");
			ArrayList<String> names = new ArrayList<String>();
			while(subclasses.next()) {
				names.add(subclasses.getString("subclass"));
			}
			subclasses.close();
			subclasses=null;
			for (String subclassname: names) {
				if (DbUtils.findToStringForClass(subclassname) ==null)  {
					subclassname = subclassname.replaceAll("\\$", ".");
					LOG.info("Parsing unresolved subclasses "+ subclassname);
					IType tp = javaproject.findType(subclassname);
					if (tp==null) {
						LOG.fine("cannot find type:: " + subclassname);
					}
				parser.parseToStringMethod(tp);
			}
			}
		}catch(Exception ex){
			LOG.severe("processUnResolvedSubClasses Exception:"+ex.getMessage());
		}
	}
}
