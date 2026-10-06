package com.alpha.base.support.linker;

import java.io.File;
import java.io.FileWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.alpha.base.context.ProjectContext;
import com.alpha.base.support.AbstractMeta;
import com.alpha.base.support.util.SystemUtilPack;
import com.alpha.base.support.util.SystemUtilPack.SystemUtil;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HealthCheckLinker extends AbstractMeta {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Hidden
	@RestController
	@ConditionalOnWebApplication
	public static class HealthCheckController {

		@Autowired
		private Environment environment;
		
		@Autowired
		private ProjectContext projectContext;
		
		@Autowired(required=false)
		private JdbcTemplate jdbcTemplate;

		@Value("${alpha.healthcheck.systemPrefix:alpha}")
		private String systemPrefix;

		@Value("${spring.application.name}")
		private String applicationName;

		@Value("${spring.profiles.active:local}")
		private String springProfilesActive;		
				
		@Value("${server.name:}")
		private String serverName;	
		
		@Value("${server.port:}")
		private String serverPort;

		@Value("${server.servlet.context-path:}")
		private String serverServletContextPath;

		@Value("${logging.file.name:}")
		private String loggingFileName;
			    
		@PostConstruct
		private void postConstruct() throws Exception {
			SystemUtil systemUtil = SystemUtilPack.getSystemUtil(this.environment,this.serverName,this.serverPort,this.applicationName);
			
			StringBuilder builder = new StringBuilder();
			builder.append("http://").append(systemUtil.getServerIp()).append(":").append(systemUtil.getServerPort()).append("/");
			
			String webContextPath="";
			if(!StringUtils.isEmpty(this.serverServletContextPath)) {webContextPath=this.serverServletContextPath;}
			if(!StringUtils.isEmpty(webContextPath)) {
				if(webContextPath.startsWith("/")) {webContextPath=webContextPath.substring(1);}
				if(!StringUtils.isEmpty(webContextPath)) {builder.append(webContextPath).append("/");}
			}
			builder.append("healthcheck");
			//log.debug(">> healthcheckUrl:{}",builder.toString());
			
			String metaFile=FilenameUtils.normalize(this.loggingFileName+".meta");
			log.info(">> healthcheck.metaFile: {}",metaFile);

			String metaPath=FilenameUtils.getPath(metaFile);
			if(!metaPath.startsWith(File.separator)) {metaPath=File.separator+metaPath;}

			if(!StringUtils.isEmpty(metaPath)) {
				File metaFolder = new File(metaPath);				
				if (!metaFolder.exists()) {metaFolder.mkdirs();}
				//log.debug(">> healthcheck.metaPath:{}",metaFolder.getAbsolutePath());
			}
			
			try(FileWriter fw = new FileWriter(new File(metaFile))){
				fw.write(builder.toString());
				fw.flush();
			}
		}

		@GetMapping(path = "/healthcheck")
		public Map<String, Object> healthcheck1(HttpServletRequest request) throws Exception {
			this.removeSession(request);
			return this.getHealthcheck();
		}

		@GetMapping(path = "/{applicationCode}/healthcheck")
		public Map<String, Object> healthcheck2(@PathVariable final String applicationCode, HttpServletRequest request) throws Exception {
			if (!this.applicationName.equals(systemPrefix + "-" + applicationCode)) {
				throw new RuntimeException("applicationCode is wrong.");
			}
			//this.removeSession(request);
			return this.getHealthcheck();
		}

		@GetMapping(path = "/{applicationCode}/availability")
		public Map<String, Object> availability(@PathVariable final String applicationCode, HttpServletRequest request) throws Exception {
			if (!this.applicationName.equals(systemPrefix + "-" + applicationCode)) {
				throw new RuntimeException("applicationCode is wrong.");
			}
			
			HttpSession session = MetaUtil.getSessionUtil().getHttpSession();
			Map<String,Object> sessionInfo = new HashMap<>();
			sessionInfo.put("sessionId", session.getId());
			sessionInfo.put("time", session.getAttribute("time"));		
			sessionInfo.put("interval", session.getMaxInactiveInterval());	
			
			Map<String,Object> healthCheck = this.getHealthcheck();
			healthCheck.put("sessionInfo", sessionInfo);
			healthCheck.put("systemUsage", MetaUtil.getSystemUtil().getSystemUsage());
			
			return healthCheck;
		}

		private void removeSession(HttpServletRequest request) {
			// 주기적인 healthcheck요청시 session이 생성되지만, session에 저장하는 AttributeNames는 없다.
			// 주기적인 healthcheck요청시 생성되는 session을 삭제한다.
			if (request.getHeader("referer") == null && request.getSession().getAttributeNames().hasMoreElements() == false) {
				request.getSession().invalidate();
			}
		}

		private Map<String,Object> getHealthcheck(){
			SystemUtil sysUtil = MetaUtil.getSystemUtil();

	        Map<String,String> deployInfo = new HashMap<>();
	        deployInfo.put("serverName",sysUtil.getServerName());
	        deployInfo.put("serverIp",sysUtil.getServerIp());
	        deployInfo.put("serverPort",sysUtil.getServerPort());
	        deployInfo.put("serverInstanceCode",sysUtil.getServerInstanceCode());

	        deployInfo.put("projectName",projectContext.getName());
	        deployInfo.put("projectDescription",projectContext.getDescription());
	        deployInfo.put("projectProfile",this.springProfilesActive);	

	        deployInfo.put("projectGroupId",projectContext.getGroupId());
	        deployInfo.put("projectArtifact",projectContext.getArtifact());
	        deployInfo.put("projectVersion",projectContext.getVersion());
	        
	        deployInfo.put("buildFinalName",projectContext.getBuild().getFinalName());
	        deployInfo.put("buildBranch",projectContext.getBuild().getBranch());
	        deployInfo.put("buildRevision",projectContext.getBuild().getRevision());
	        deployInfo.put("buildTimestamp",projectContext.getBuild().getTimestamp());

	        for(String key:deployInfo.keySet()){
	        	if(deployInfo.get(key)!=null && deployInfo.get(key).startsWith("@") && deployInfo.get(key).endsWith("@")) {
	        		deployInfo.put(key,"");
	        	}
	        }
	        
	        String healthcheck="ok";
	        if(this.jdbcTemplate!=null) {
	        	healthcheck = this.jdbcTemplate.query(
        			"select now()"
        			,new ResultSetExtractor<String>() {
        				@Override
        				public String extractData(ResultSet rs) throws SQLException, DataAccessException {
        					rs.next();
        					return rs.getString(1);
        				}
        			}
        		);	        	
	        }

	        Map<String,Object> result = new HashMap<>();        
	        result.put("healthcheck", healthcheck);
	        result.put("deployInfo", deployInfo);

	        return result;
		}
	};

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}
