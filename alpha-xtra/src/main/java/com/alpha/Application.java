package com.alpha;

import javax.servlet.http.HttpServletRequest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;

import com.alpha.base.config.AidBaseConfig.AidImportSelector;

@SpringBootApplication
@Import(AidImportSelector.class)
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	public static class DefaultBinder {

		@InitBinder
		public void initBinder(WebDataBinder binder,HttpServletRequest request) {
			binder.setDisallowedFields(new String[]{"serialVersionUID"});
		}
	}
}


