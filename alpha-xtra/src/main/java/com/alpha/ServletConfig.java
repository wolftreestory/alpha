package com.alpha;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServletConfig {
	
	@Bean
	ServletRegistrationBean<HelloServlet> getHelloServletRegistrationBean() {
		ServletRegistrationBean<HelloServlet> registrationBean = new ServletRegistrationBean<>(new HelloServlet());

		registrationBean.addUrlMappings("/xtra/hello/*");
	
		return registrationBean;
	}
}

class HelloServlet extends HttpServlet{

	private static final long serialVersionUID = 1L;

	@Override
	public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException{
		res.getWriter().print("hello");
	}
}