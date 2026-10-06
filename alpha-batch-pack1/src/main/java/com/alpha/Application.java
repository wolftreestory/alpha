package com.alpha;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import com.alpha.base.config.AidBaseConfig.AidImportSelector;

@EnableBatchProcessing
@SpringBootApplication
@Import(AidImportSelector.class)
public class Application {

	public static void main(String[] args) {
		System.exit(SpringApplication.exit(SpringApplication.run(Application.class, args)));
	}

}
