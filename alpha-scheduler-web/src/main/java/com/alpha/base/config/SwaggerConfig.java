package com.alpha.base.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
@ConditionalOnExpression("T(com.alpha.base.SwaggerConfig).isEnabled()")
@ConditionalOnProperty(name="springdoc.swagger-ui.enabled", havingValue="true", matchIfMissing=false)
@ConditionalOnWebApplication
public class SwaggerConfig {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Value("${spring.profiles.active:local}")
	private String springProfilesActive;
	
	@Value("${project.name:project.name}")
    private String name;

    @Value("${project.description:project.description}")
    private String description;

    @Value("${project.version:project.version}")
    private String version;

    @Value("${alpha.security.enabled:false}")
    private boolean isSecurity;
    
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Bean
    OpenAPI openAPI() {
    	
    	Info info = new Info();
    	info.setTitle(this.name+" : "+this.springProfilesActive);
    	info.setDescription(this.description);
    	info.version(this.version);
      	
    	OpenAPI openApi = new OpenAPI();
        openApi.setInfo(info);

        if(this.isSecurity) {
        	SecurityScheme securitySchemesItem = new SecurityScheme()
    			   .type(SecurityScheme.Type.HTTP)
    			   .in(SecurityScheme.In.HEADER)
    			   .name("Authorization")
    			   .bearerFormat("JWT")
    			   .scheme("Bearer");

        	openApi.components(new Components().addSecuritySchemes("Bearer Token", securitySchemesItem));
        	openApi.addSecurityItem(new SecurityRequirement().addList("Bearer Token"));
        }
        
    	return openApi;
    }

//    @Bean
//    OpenAPI openAPI() {
//    	
//    	OpenAPI openApi = new OpenAPI();
//    	
//    	if(this.springProfilesActive.equals("dev")){
//	    	Server server1 = new Server();
//	    	server1.setUrl("http://dev.domain.co.kr/");
//	    	server1.setDescription("DEV-HTTP");
//	    	openApi.addServersItem(server1);
//	    	
//    		Server server2 = new Server();
//    		server2.setUrl("https://dev.domain.co.kr/");
//    		server2.setDescription("DEV-HTTPS");
//	    	openApi.addServersItem(server2);
//    	}
//    	
//    	if(this.springProfilesActive.equals("stg")){
//	    	Server server1 = new Server();
//	    	server1.setUrl("http://stg.domain.co.kr/");
//	    	server1.setDescription("STG-HTTP");
//	    	openApi.addServersItem(server1);
//	    	
//    		Server server2 = new Server();
//    		server2.setUrl("https://stg.domain.co.kr/");
//    		server2.setDescription("STG-HTTPS");
//	    	openApi.addServersItem(server2);
//    	}
//    	
//    	if(this.springProfilesActive.equals("prod")){	
//	    	Server server1 = new Server();
//	    	server1.setUrl("http://prod.domain.co.kr/");
//	    	server1.setDescription("PROD-HTTP");
//	    	openApi.addServersItem(server1);
//	    	
//    		Server server2 = new Server();
//    		server2.setUrl("https://prod.domain.co.kr/");
//    		server2.setDescription("PROD-HTTPS");
//	    	openApi.addServersItem(server2);
//    	}
//    	
//    	
//    	Info info = new Info();
//    	info.setTitle(this.name+"-"+this.springProfilesActive);
//    	info.setDescription(this.description);
//    	info.version(this.version);
//    
//    	openApi.setInfo(info);
//    	
//    	return openApi;
//    }
    
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
    *
    @Api → @Tag
    @ApiIgnore → @Parameter(hidden = true) or @Operation(hidden = true) or @Hidden
    @ApiImplicitParam → @Parameter
    @ApiImplicitParams → @Parameters
    @ApiModel → @Schema
    @ApiModelProperty(hidden = true) → @Schema(accessMode = READ_ONLY)
    @ApiModelProperty → @Schema
    @ApiOperation(value = "foo", notes = "bar") → @Operation(summary = "foo", description = "bar")
    @ApiParam → @Parameter
    @ApiResponse(code = 404, message = "foo") → @ApiResponse(responseCode = "404", description = "foo") 
    *
	**/
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
}

