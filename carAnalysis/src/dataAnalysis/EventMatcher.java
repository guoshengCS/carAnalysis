package dataAnalysis;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javabean.ConceptTable;
import javabean.MatchedResult;
import javabean.db.Event;
import javabean.db.KeywordGroup;
import javabean.db.Template;
/**
 * ���ݴ�����¼���һ���ı�Ƭ�ν���ƥ��
 * @author syq
 */
public class EventMatcher {
	/**
	 * ��ǰ���¼�ģ��
	 */
	private Template t=null;
	/**
	 * ���Ŷ�Ӧ��Ĭ�Ͼ���
	 */
	private int parentheses_distance = 8;
	/**
	 * �ӺŶ�Ӧ��Ĭ�Ͼ���
	 */
	private int plus_distance = 8;
	
	/**
	 * �Ƿ���ʾ������Ϣ
	 */
    private boolean isTest = false;



	/**
	 * @return find���������¼�����ģ�尤��ƥ�䣬һ��ƥ��ɹ����ص�һ����
	 */
    /**
     * �����¼�����ģ�尤��ƥ�䣬һ��ƥ��ɹ����ص�һ����
     * @param src ��ƥ���ԭ�ļ�Ƭ��
     * @param e ��ǰƥ�����õ��¼�
     * @param pageId ��ǰ�ı�Ƭ������ҳ��
     * @param templates ��ǰ�¼�ʣ��ģ��
     * @return ƥ�䵽�Ľ��
     */
	public ArrayList<MatchedResult> find(String src,Event e,String pageId,ArrayList<Template> templates) {
		if(isTest){
			System.out.println(src+"::::"+e.getEventID());
			System.out.println("ģ��������"+templates.size());
		}
		while(templates.size()>0)
		{
//			for(KeywordGroup k : templates.get(i).getConcepts())	//ƥ��ģ����������и���
//			{
//				getConceptmap(k,templates.get(i),src,used,pageId);
//			}
			

			ArrayList<String> op_queue = new ArrayList<String>();
			Stack<ConceptTable> table_stack = new Stack<ConceptTable>();
			String rule = templates.get(0).getTemplateRule();
			if(isTest)
				System.out.println("ģ�����"+rule);
			Pattern templateP = Pattern.compile("(\\{\\d*?\\}|\\(|\\)|\\[|\\]|\\+)");
			String[] templaterule = templateP.split(rule);
			int null_num = 0;
			//���ַ�����ʾ�ĸ���ת��Ϊ��table��ʾ�ĸ���,������ѹ��һ����table���ɵı�ջ
			//ͬʱ����������˳�����һ������
			if(isTest)
				System.out.print("���");
			boolean isFind = false;
			for(int rule_count=templaterule.length-1;rule_count>=0;rule_count--){
				if(templaterule[rule_count].length()==0){
					null_num++;
					continue;
				}
				isFind = false;
				if(e.getConceptMap().containsKey(templaterule[rule_count]))
				{
					KeywordGroup kwg = e.getConceptByName(templaterule[rule_count]);
					getConceptmap(kwg,src,pageId);

					if(!kwg.getResultMaps().containsKey(pageId)){
						System.err.println(pageId+"�Ҳ���\""+templaterule[rule_count]+"\"in\t"+rule);
					}
					table_stack.push(kwg.getResultMaps().get(pageId));
					if(isTest){
						System.out.print(table_stack.peek().getConceptName()+"\t");
					}
					isFind = true;
				}
				else if(!isFind&&WebPageAnalyzer.global_concept.containsKey(templaterule[rule_count])){
					KeywordGroup kwg = WebPageAnalyzer.global_concept.get(templaterule[rule_count]);
					getConceptmap(kwg,src,pageId);

					if(!WebPageAnalyzer.global_concept.get(templaterule[rule_count]).getResultMaps().containsKey(pageId)){
						System.err.println(pageId+"�Ҳ�����ͨ�ã�"+templaterule[rule_count]);
					}
					table_stack.push(WebPageAnalyzer.global_concept.get(templaterule[rule_count]).getResultMaps().get(pageId));
					isFind = true;
					if(isTest)
						System.out.print(table_stack.peek().getConceptName()+"(ͨ��)"+"\t");
				}
				if(!isFind)
					System.err.println("����:\""+templaterule[rule_count]+"\"�Ҳ���");
			}
			if(isTest)
				System.out.println();
			if(table_stack.size()<(templaterule.length-null_num)){
				System.err.println(rule+"���ȫ���޷�ƥ��");
				templates.remove(0);			
				continue;
			}
//			System.out.println("ջ��С"+table_stack.size());
				
			Matcher templateM = templateP.matcher(rule);
			while(templateM.find()){
				op_queue.add(templateM.group());
			}

//			System.out.print("��������");
//			for(String op_test:op_queue){
//				System.out.print(op_test+"\t");
//			}
//			System.out.print("\r\n");
			
			int op_count=0;
			while(op_count<op_queue.size()){
				if(op_queue.get(op_count).equals("(")){
					op_count+=doLittleParentheses(op_queue,op_count,table_stack);
				}
				else if(op_queue.get(op_count).equals("[")){
					op_count+=doMidParentheses(op_queue,op_count,table_stack);
				}
				else{
					op_count+=doPlus(op_queue,op_count,table_stack);
				}
			}
			//ĳ�������Ѿ�Ϊ����ֱ�ӽ�������ƥ��
			if(op_count>op_queue.size()){				
				templates.remove(0);			
				continue;
			}
			ConceptTable resultTable= table_stack.pop();
			ArrayList<MatchedResult> resultList = resultTable.getStartResultMaps();
			t = templates.get(0);
			for(MatchedResult mr:resultList){
				mr.setT(t);
			}
			if(resultList.size()>0){
				System.out.println("ƥ�䵽ģ�棺"+resultTable.getConceptName()+"���Ϊ:"+src.substring(resultList.get(0).getStart(),resultList.get(0).getEnd()));
				templates.remove(0);
				return resultList;
			}
			templates.remove(0);			
		}
		if(isTest)
			System.out.println("û��ƥ�䵽�¼�");
		return null;
		
		
		
			
//		
//			Pattern p = Pattern.compile("\\[(.*?)\\]\\(.*?\\)");
//			Matcher m = p.matcher(templates.get(i).getTemplateRule());
//			while(m.find())
//			{
//				String concept_one =m.group(1);
//				String concept_two =m.group(2);
//				KeywordGroup one = null,two=null;
//				for(KeywordGroup k : templates.get(i).getConcepts())	
//				{
//					if(k.getKeywordGroupName().equals(concept_one))
//					{
//						one = k;continue;
//					}
//					if(k.getKeywordGroupName().equals(concept_two))
//					{
//						two = k;
//					}
//				}
//				HashMap<String,MatchedResult> one_maps =one.getStartResultMaps();
//				HashMap<String,MatchedResult> two_maps =one.getStartResultMaps();	
//			}
			
			

	}
	
	/**
	 * @author Bys
	 * @param op_queue
	 * @param op_count
	 * @param table_stack
	 * @return �ӱ�ջȡ����Ӧ���������[A+B+��](C)�Ĳ����������ѹ��ջ�����ز����õ�����������
	 */
	private int doMidParentheses(ArrayList<String> op_queue,int op_count,Stack<ConceptTable> table_stack){
		if(op_count>op_queue.size())
			return op_count;
//		System.out.println("ջ��С"+table_stack.size());
		int offset = 1;
		while(!op_queue.get(op_count+offset).equals("]")){
			if(op_queue.get(op_count+offset).equals("(")){
				offset+=doLittleParentheses(op_queue,op_count+offset,table_stack);
			}
			else{
				offset+=doPlus(op_queue,op_count+offset,table_stack);
			}
			if(op_count+offset>op_queue.size())
				return op_queue.size()+1;
		}
		ConceptTable tmpTable = table_stack.pop();
		if(!isTest&&tmpTable.getStartResultMaps().size()==0){
			System.out.println("\""+tmpTable.getConceptName()+"\"ƥ��Ϊ�գ�");
			return op_queue.size()+1;
		}
		tmpTable.setConceptName("["+tmpTable.getConceptName()+"]");
		if(isTest)
			System.out.println(tmpTable.getConceptName());
		table_stack.push(tmpTable);
		offset++;
		if(!op_queue.get(op_count+offset).equals("(")){
			System.err.println("���ʽ����![]����()");
			return offset;
		}
		else{
			offset+=doLittleParentheses(op_queue,op_count+offset,table_stack);
			return offset;
		}
	}
	
	
	/**
	 * @author Bys
	 * @param op_queue
	 * @param op_count
	 * @param table_stack
	 * @return �����ּӺſ�ʼһֱ������мӣ�ֱ������]���߲������þ�
	 */
	private int doPlus(ArrayList<String> op_queue,int op_count,Stack<ConceptTable> table_stack){
		if(op_count>op_queue.size())
			return op_count;
		
		
//		System.out.println("ջ��С"+table_stack.size());
		int offset = 1;
		ConceptTable tmpTable = new ConceptTable();
		if((op_count+offset)==op_queue.size()||op_queue.get(op_count+offset).equals("]")||op_queue.get(op_count+offset).equals(")")){
			tmpTable = table_stack.pop();
			if(op_queue.get(op_count).equals("+"))
				table_stack.push(plus_op(tmpTable,table_stack.pop()));
			else{
				int distance = Integer.parseInt(op_queue.get(op_count).replaceAll("\\{|\\}", ""));
				table_stack.push(plus_op(tmpTable,table_stack.pop(),distance));
//				offset++;
			}
			tmpTable = table_stack.peek();
			if(!isTest&&tmpTable.getStartResultMaps().size()==0){
//				System.out.println("\""+tmpTable.getConceptName()+"\"ƥ��Ϊ�գ�");
				return op_queue.size()+1;
			}
			return offset;
		}
		else if(op_queue.get(op_count+offset).equals("(")){
			tmpTable = table_stack.pop();
			if(!isTest&&tmpTable.getStartResultMaps().size()==0){
//				System.out.println("\""+tmpTable.getConceptName()+"\"ƥ��Ϊ�գ�");
				return op_queue.size()+1;
			}
			offset+=doLittleParentheses(op_queue,op_count+offset,table_stack);
			if((op_count+offset)==op_queue.size()||op_queue.get(op_count+offset).equals("]")){
				return offset;
			}
			else{
				offset+=doPlus(op_queue,op_count+offset,table_stack);
				return offset;
			}
		}
		else if(op_queue.get(op_count+offset).equals("[")){
			tmpTable = table_stack.pop();
			if(!isTest&&tmpTable.getStartResultMaps().size()==0){
//				System.out.println("\""+tmpTable.getConceptName()+"\"ƥ��Ϊ�գ�");
				return op_queue.size()+1;
			}
			offset+=doMidParentheses(op_queue,op_count+offset,table_stack);
			if(op_queue.get(op_count).equals("+"))
				table_stack.push(plus_op(tmpTable,table_stack.pop()));
			else{
				int distance = Integer.parseInt(op_queue.get(op_count).replaceAll("\\{|\\}", ""));
				table_stack.push(plus_op(tmpTable,table_stack.pop(),distance));
//				offset++;
			}
			if((op_count+offset)==op_queue.size()||op_queue.get(op_count+offset).equals("]")){
				return offset;
			}
			else{
				offset+=doPlus(op_queue,op_count+offset,table_stack);
				return offset;
			}
		}
		else{
			tmpTable = table_stack.pop();
			if(!isTest&&tmpTable.getStartResultMaps().size()==0){
//				System.out.println("\""+tmpTable.getConceptName()+"\"ƥ��Ϊ�գ�");
				return op_queue.size()+1;
			}
			offset+=doPlus(op_queue,op_count+offset,table_stack);
			if(op_queue.get(op_count).equals("+"))
				table_stack.push(plus_op(tmpTable,table_stack.pop()));
			else{
				int distance = Integer.parseInt(op_queue.get(op_count).replaceAll("\\{|\\}", ""));
				table_stack.push(plus_op(tmpTable,table_stack.pop(),distance));
//				offset++;
			}
			return offset;
		}
	}
	
	
	/**
	 * @author Bys
	 * @param op_queue
	 * @param op_count
	 * @param table_stack
	 * @return �ӱ�ջ��ȡ��2�����������A(B)����A({n}B)���������ѹ��ջ
	 */
	private int doLittleParentheses(ArrayList<String> op_queue,int op_count,Stack<ConceptTable> table_stack){
		if(op_count>op_queue.size())
			return op_count;
//		System.out.println("ջ��С"+table_stack.size());
		int offset = 1;
		ConceptTable tmpTable = new ConceptTable();
		if(op_queue.get(op_count+offset).equals(")")){
			tmpTable = parentheses_op(table_stack.pop(),table_stack.pop());
			if(!isTest&&tmpTable.getStartResultMaps().size()==0){
//				System.out.println("\""+tmpTable.getConceptName()+"\"ƥ��Ϊ�գ�");
				return op_queue.size()+1;
			}
			table_stack.push(tmpTable);
			offset++;
			return offset;
		}
		else if(op_queue.get(op_count+offset).substring(0, 1).equals("{")){
			int distance = Integer.parseInt(op_queue.get(op_count+offset).replaceAll("\\{|\\}", ""));
			tmpTable = parentheses_op(table_stack.pop(),table_stack.pop(),distance);
			if(!isTest&&tmpTable.getStartResultMaps().size()==0){
//				System.out.println("\""+tmpTable.getConceptName()+"\"ƥ��Ϊ�գ�");
				return op_queue.size()+1;
			}
			table_stack.push(tmpTable);
			offset+=2;
//			offset++;
			return offset;
		}
		else if(op_queue.get(op_count+offset).equals("+")){
			tmpTable = table_stack.pop();
			offset+=doPlus(op_queue,op_count+offset,table_stack);
			tmpTable = parentheses_op(tmpTable,table_stack.pop());
			if(!isTest&&tmpTable.getStartResultMaps().size()==0){
//				System.out.println("\""+tmpTable.getConceptName()+"\"ƥ��Ϊ�գ�");
				return op_queue.size()+1;
			}
			table_stack.push(tmpTable);
			offset++;
			return offset;
		}
		System.err.println("С�������������������");
		offset++;
		return offset;
	}
	
	
	/**
	 * @author Bys
	 * @param ct1 С��������������������Ϊһ��λ�ñ�
	 * @param ct2 С������������ڲ�������Ϊһ��λ�ñ�
	 * @return ���λ�ñ�����A(B)�ĸ�����ֵ�λ��,ȱʡ����parentheses_distance=8
	 */
	private ConceptTable parentheses_op(ConceptTable ct1,ConceptTable ct2){
		ConceptTable result = parentheses_op(ct1,ct2,parentheses_distance);
		result.setConceptName(ct1.getConceptName()+"("+ct2.getConceptName()+")");
		if(isTest)
			System.out.println(result.getConceptName());
		return result;
	}
	
	
	/**
	 * @author Bys
	 * @param ct1 С��������������������Ϊһ��λ�ñ�
	 * @param ct2 С������������ڲ�������Ϊһ��λ�ñ�
	 * @param distance 2������֮��ľ���
	 * @return ���λ�ñ�����A({distance}B)�ĸ�����ֵ�λ��
	 */
	private ConceptTable parentheses_op(ConceptTable ct1,ConceptTable ct2,int distance){
		ArrayList<MatchedResult> resultList = new ArrayList<MatchedResult>(0);
		boolean isStart = false;
		MatchedResult scope = new MatchedResult();
		for(MatchedResult mr:ct1.getStartResultMaps()){
			isStart = false;
			scope.setStart(0);
			scope.setEnd(mr.getEnd()+distance);
			
			for(MatchedResult mr2:ct2.getStartResultMaps()){
				if(mr.cross(mr2)){
					continue;
				}
//				������ڷ�Χ�ڣ���������ʼ����Ҳ���ǳ�����Χ���������ض�Ҳ������Χ
				else if(!scope.contains(mr2)){
					if(isStart)
						break;
					}
//				���mr2��mr��չ��ķ�Χ�н�������mr��mr2�ϲ����迪ʼ���Ϊ��
				else{
					if(mr2.getEnd()>=mr.getStart()-distance){
						MatchedResult temp = new MatchedResult(mr.getStart()<mr2.getStart()?mr.getStart():mr2.getStart(),
								mr.getEnd()>mr2.getEnd()?mr.getEnd():mr2.getEnd());
//						if(temp.getEnd()-temp.getStart()>20)
//							System.err.println("????");
						if(temp.getEnd()-temp.getStart()<30){
							resultList.add(temp);
							isStart = true;
						}
					}
				}
			}
		}
		scope = null;
		if(isTest&&distance!=parentheses_distance)
			System.out.println(ct1.getConceptName()+"({"+distance+"}"+ct2.getConceptName()+")");
		return new ConceptTable(resultList,ct1.getConceptName()+"({"+distance+"}"+ct2.getConceptName()+")");
	}
	
	/**
	 * @author Bys
	 * @param ct1 �ӷ�����������������Ϊһ��λ�ñ�
	 * @param ct2 �ӷ���������Ҳ�������Ϊһ��λ�ñ�
	 * @return ���λ�ñ�����A+B�ĸ�����ֵ�λ�á�ȱʡֵplus_distance=8
	 */
	private ConceptTable plus_op(ConceptTable ct1,ConceptTable ct2){
		ConceptTable result = plus_op(ct1,ct2,plus_distance);
		result.setConceptName(ct1.getConceptName()+"+"+ct2.getConceptName());
		if(isTest)
			System.out.println(result.getConceptName());
		return result;
	}
	
	/**
	 * @author Bys
	 * @param ct1 �ӷ�����������������Ϊһ��λ�ñ�
	 * @param ct2 �ӷ���������Ҳ�������Ϊһ��λ�ñ�
	 * @param distance �ӷ���Ӧ��2������֮��ľ���
	 * @return ���λ�ñ�����A{distance}B�ĸ�����ֵ�λ��
	 */
	private ConceptTable plus_op(ConceptTable ct1,ConceptTable ct2,int distance){
		ArrayList<MatchedResult> resultList = new ArrayList<MatchedResult>(0);
		boolean isStart = false;
		MatchedResult scope = new MatchedResult();
		for(MatchedResult mr:ct1.getStartResultMaps()){
			isStart = false;
			scope.setStart(mr.getEnd());
			scope.setEnd(mr.getEnd()+distance);
			
			for(MatchedResult mr2:ct2.getStartResultMaps()){
				if(mr.cross(mr2)){
					continue;
				}
//				������ڷ�Χ�ڣ���������ʼ����Ҳ���ǳ�����Χ���������ض�Ҳ������Χ
				if(!scope.contains(mr2)){
					if(isStart)
						break;
				}
//				���mr2��mr��չ��ķ�Χ�н�������mr��mr2�ϲ����迪ʼ���Ϊ��
				else{
					MatchedResult temp = new MatchedResult(mr.getStart()<mr2.getStart()?mr.getStart():mr2.getStart(),
							mr.getEnd()>mr2.getEnd()?mr.getEnd():mr2.getEnd());						
//					if(temp.getEnd()-temp.getStart()>20)
//								System.err.println("????");
					if(temp.getEnd()-temp.getStart()<30){
						resultList.add(temp);
						isStart = true;
					}
				}
			}
		}
		scope = null;
		if(isTest&&distance!=plus_distance)
			System.out.println(ct1.getConceptName()+"{"+distance+"}"+ct2.getConceptName());
		return new ConceptTable(resultList,ct1.getConceptName()+"{"+distance+"}"+ct2.getConceptName());
	}
	
	
	/**
	 * ��ȡ�������ı�ƥ��ӳ��
	 * @param k ����
	 * @param src �������ı� 
	 * @param pageId 
	 */
    public void getConceptmap(KeywordGroup k,String src,String pageId)   //��ȡ������ı�ƥ��ӳ��
    {
		ArrayList<MatchedResult> concept_result = new ArrayList<MatchedResult>(0);
    	if(k.getResultMaps().containsKey(pageId)){
//    		if(!WebPageAnalyzer.usedMap.containsKey(pageId))
//        		System.err.println("?????????????????????????????????????????????????????????????????????");

//    		if(WebPageAnalyzer.conceptMap.containsKey(k.getKeywordGroupName()))
//    			System.err.println(pageId+"����һ�����"+k.getKeywordGroupName());
//    		else if(WebPageAnalyzer.global_concept.containsKey(k.getKeywordGroupName()))
//    			System.err.println(pageId+"����ͨ�ø���"+k.getKeywordGroupName());
    		return;
    	}
    	else
    	{

//    		System.err.println(k.equals(WebPageAnalyzer.conceptMap.get(k).getKeywordGroupName()));
    		if(k.getChildKeywordGroup().size()>0)
    		{
    		   List<KeywordGroup> childs =k.getChildKeywordGroup();
    		   for(KeywordGroup ck:childs)
    		   {
    			   getConceptmap(ck,src,pageId);
    			   ConceptTable childTable = ck.getResultMaps().get(pageId);
    			   concept_result.addAll(childTable.getStartResultMaps());
    		   }
    		}
    		else
    		{
    			Pattern p = Pattern.compile(k.generateExp());
    			Matcher m = p.matcher(src);
    			while(m.find())
    			{
    				concept_result.add(new MatchedResult(m.start(),m.end()));
    				if(isTest){
    					System.out.println("ƥ�䵽�ؼ��ʣ�"+m.group()+m.start()+"-"+m.end());
    				}
    			}
    		}    		
//    		Vector<String> used = WebPageAnalyzer.usedMap.get(pageId);
//    		if(used.contains(k.getKeywordGroupName()))        		
//    			System.err.println("?????????????????????????????????????????????????????????????????????");

//    		if(!used.contains(k.getKeywordGroupName()))
//    			used.add(k.getKeywordGroupName());
    		k.getResultMaps().put(pageId, new ConceptTable(concept_result,k.getKeywordGroupName()));
//    		System.err.println(k.getKeywordGroupName()+":"+k.getResultMaps().size());

    	}
    }
    
    

}
