package batman.utils;

import java.io.BufferedReader;

public abstract class AbstractLineProcessor {
	private String tokenizationExpression = "\\t";
	private boolean processComments = false;
	private char commentChar = '#';
	private boolean commentsAnywhere = false;
	private boolean ignoreEmptyLines = true;
	
	public void setProcessComments(boolean b) {
		this.processComments = b;
	}
	
	public void setCommentChar(char c) {
		this.commentChar = c;
	}
	
	public void setCommentsAnywhere(boolean b) {
		this.commentsAnywhere = b;
	}
	
	public void setIgnoreEmptyLines(boolean b) {
		this.ignoreEmptyLines = b;
	}
	
	
	public void setTokenizationExpression(String s) {
		this.tokenizationExpression = s;
	}
	
	public String getTokenizationExpression() {
		return tokenizationExpression;
	}
	
	public void main(String[] args)
		throws Exception
	{
		int count = 0;
		pre();
		BufferedReader br = IOTools.inputBufferedReader(args);
		for (String line = br.readLine(); line != null; line = br.readLine()) {
			try {
				++count;
				if (processComments) {
					line = stripComments(line);
					if (ignoreEmptyLines && line.length() == 0) {
						continue;
					}
				} 
				processLine(line);
			} catch (Exception e) {
				Exception e2 = new Exception("Error processing line " + count);
				e2.initCause(e);
				throw e2;
			}
		}
		post();
	}
	
	public String stripComments(String s)
		throws Exception
	{
		int i = s.indexOf(commentChar);
		if (i == 0) {
			return "";
		} else if (commentsAnywhere && i > 0) {
			return s.substring(0, i);
		} else {
			return s;
		}
	}
	
	public void processLine(String line) throws Exception  {
		processTokens(tokenize(line));
	}
	
	public void processTokens(String[] toks) throws Exception {
	}
	
	public String[] tokenize(String line) throws Exception {
		return line.split(getTokenizationExpression());
	}
	
	public void pre() throws Exception {
	}
	
	public void post() throws Exception {
	}
}
