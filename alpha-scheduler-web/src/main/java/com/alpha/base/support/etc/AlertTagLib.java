package com.alpha.base.support.etc;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import com.alpha.base.BaseUtil;

public class AlertTagLib extends TagSupport {
	
	private static final long serialVersionUID=1L;
	
	private String alertMessageKey=null;
	
	public AlertTagLib() {
		this.alertMessageKey= BaseUtil.getAlertUtil().getAlertMessageKey();
	}

	public int doEndTag() throws JspException {
		if(this.alertMessageKey==null) {throw new JspException("alertStringKey is null");}

        Object messageObj = pageContext.getSession().getAttribute(this.alertMessageKey);
        if(messageObj==null) {return EVAL_PAGE;}

        try {
            pageContext.getOut().write(this.getScriptAlert(messageObj));
            pageContext.getSession().setAttribute(this.alertMessageKey, null); //1회성 Alert
        } catch (Exception e) {
            throw new JspException("IO Error: " + e.getMessage());
        }

        return EVAL_PAGE;
    }
	
	private String getScriptAlert(Object message) {
		StringBuilder builder = new StringBuilder();
		builder.append("<script language=\"javascript\">").append("\n");
		builder.append("var _existingOnload = window.onload;").append("\n");
		builder.append("var _alertMsg = \"").append(message.toString()).append("\"").append("\n");
		builder.append("window.onload = function() {").append("\n");
		builder.append("if(_existingOnload){_existingOnload();}").append("\n");
		builder.append("if(_alertMsg){setTimeout(() => {alert(_alertMsg);_alertMsg = null;}, 750);}").append("\n");
		builder.append("}").append("\n");
		builder.append("</script>").append("\n");

		return builder.toString();
	}
	
}
