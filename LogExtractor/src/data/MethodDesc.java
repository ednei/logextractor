package data;

import java.io.Serializable;
//import java.util.logging.Logger;

@SuppressWarnings("serial")
public class MethodDesc implements Serializable, Comparable<MethodDesc>{
	public String methodclass;
	public String methodname;
	public String params;
	public String returntype;
	//private static Logger log = Logger.getLogger(MethodEdge.class.getName());
	
	public MethodDesc(String methodclass, String methodname, String params,
			String returntype, int dbRecId) {
		super();
		this.methodclass = methodclass;
		this.methodname = methodname;
		this.params = params;
		this.returntype = returntype;
	}
	
	public MethodDesc(String methodclass, String methodname, String params,
			String returntype) {
		this(methodclass, methodname, params, returntype, -1);
	}
	
	@Override
	public boolean equals(Object arg0) {
		MethodDesc th = (MethodDesc) arg0;
		return this.methodclass.equals(th.methodclass) 
		&& this.methodname.equals(th.methodname)
		&& this.params.equals(th.params);
	}

	public int compareTo(MethodDesc o) {
		MethodDesc th = (MethodDesc) o;
		int t = this.methodclass.compareTo(th.methodclass);
		if (t==0) {
			return this.methodname.compareTo(th.methodname);
		} else 
			return t;
	}
	
	public String toString() {
		return "MethodDesc(class:"+methodclass
				+";method:"+methodname
				+";params:"+params
				+";returnType:"+returntype+")";
	}
	
	public void writeToDB() {
		//log.info("Saving on DB:"+toString());
		//TODO 	
		//DbUtils.insertMethod(methodclass, methodname, params, returntype);
	}
	
	public int findMethodAutoId() {
		//TODO 
		//if (this.dbRecId!=-1) {
		//	return this.dbRecId;
		//}
		//this.dbRecId = DbUtils.findMethodId(methodclass, methodname, params);
		//return this.dbRecId;
		return 0;
	}
	
}
