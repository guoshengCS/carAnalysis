package dataAnalysis;

import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * ����String����WithReglExpr��������Pattern�������������һ��hashMap����
 * @author Bys
 *
 */
public class PatternAgent {
	 static HashMap<Object,Pattern> pmap =new HashMap<Object,Pattern>();
	
	public static Pattern getPattern(Object o) {
		if(pmap==null){
			pmap = new HashMap<Object,Pattern>();
		}
		if (pmap.containsKey(o))
			return pmap.get(o);
		else {
			Pattern p = loadPattern(o);
			pmap.put(o, p);
			return p;
		}
	}

	private static Pattern loadPattern(Object o) {
		// TODO Auto-generated method stub
		String exp = null;
		if (o instanceof String)
			exp = (String)o;
		else if (o instanceof WithReglExpr)
			exp = ((WithReglExpr)o).generateExp();
		Pattern p = exp==null?null:Pattern.compile(exp);
		if (p==null)
			System.out.println("[���ؾ���] loadPattern()�Ĳ���������String��WithReglExpr����");
		return p;
	}
	
	
}
