package com.alpha.batch.meta;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.StandardLocation;

import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import com.google.auto.service.AutoService;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class SchedulerMetaPack extends AbstractSchedulerMetaTaskInfoPack {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.SOURCE)
	public static @interface SchedulerMetaInfo {
		public String jobName();
		public String description() default "";
		public ScheduleType scheduleType() default ScheduleType.NONE;
		public String scheduleValue() default "";		
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@AutoService(Processor.class)
	@SupportedAnnotationTypes("com.alpha.batch.meta.SchedulerMetaPack.SchedulerMetaInfo")
	public static class SchedulerMetaInfoProcessor extends AbstractProcessor {
		
	    private static final Pattern JOB_PARAM_PATTERN = Pattern.compile("^#\\{jobParameters\\[.*\\].*\\}$");  
	    private static final String META_FILE = "schedulerMetaInfo.json";

	    @Override
	    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
	        Set<TypeElement> annotatedElements = ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(SchedulerMetaInfo.class));
	        if (annotatedElements.isEmpty()) return false;

	        String projectName = this.extractProjectName();
	        
	        JsonArray jsonArray = new JsonArray();
	        annotatedElements.forEach(element -> jsonArray.add(this.createJsonObject(projectName, element, roundEnv)));

	        Set<String> jobNames = new HashSet<>();
	        for (JsonElement element : jsonArray) {
	            JsonObject jsonObject = element.getAsJsonObject();
	            String jobName = jsonObject.get("jobName").getAsString();
	            if (!jobNames.add(jobName)) {
	            	String errorMsg = "중복된 jobName: " + jobName;
	                System.out.println(errorMsg);
	            	throw new RuntimeException(errorMsg);
	            }
	        }
	        
	        this.writeJsonToMetaInf(new Gson().toJson(jsonArray));
	        
	        return true;
	    }

	    private JsonObject createJsonObject(String projectName, TypeElement element, RoundEnvironment roundEnv) {        
	    	String className = ((TypeElement) element).getQualifiedName().toString();    			

	        SchedulerMetaInfo annotationInfo = element.getAnnotation(SchedulerMetaInfo.class);
	        String jobName = annotationInfo.jobName();
	        
	    	Set<String> beanNames = new HashSet<>();
	    	Set<String> jobParametersSet = new HashSet<>();
	        roundEnv.getRootElements().stream()
	        	.filter(e -> element.getAnnotation(SchedulerMetaInfo.class) != null)
	        	.filter(e -> ((TypeElement) e).getQualifiedName().toString().equals(className))
	        	.forEach(e -> {
	    	    	//System.out.println(className);

		            // @Bean : 메서드
		            ElementFilter.methodsIn(e.getEnclosedElements()).stream()
		            	.filter(_m -> _m.getAnnotation(Bean.class) != null)
	                    .forEach(_m -> {
	                    	Bean bean = _m.getAnnotation(Bean.class);	                    	
	                    	beanNames.addAll(new HashSet<>(Arrays.asList(bean.name())));
	                    	beanNames.addAll(new HashSet<>(Arrays.asList(bean.value())));
	                    	});
		            
		            // @Value : 필드
		            ElementFilter.fieldsIn(e.getEnclosedElements()).stream()
		            	.filter(_f -> this.hasValidJobParameterPattern(_f.getAnnotation(Value.class)))
		            	.map(_f -> this.extractJobParameter(_f))
		            	.forEach(jobParameter -> jobParametersSet.add(jobParameter));
		
		            // @Value : 메서드-파라미터
		            ElementFilter.methodsIn(e.getEnclosedElements()).stream()
	                    .flatMap(_p -> _p.getParameters().stream())
	                    .filter(_p -> this.hasValidJobParameterPattern(_p.getAnnotation(Value.class)))
	                    .map(_p -> this.extractJobParameter(_p))
	                    .forEach(jobParameter -> jobParametersSet.add(jobParameter));
		
		            // @Value : 생성자-파라미터
		            ElementFilter.constructorsIn(e.getEnclosedElements()).stream()
	                    .flatMap(_c -> _c.getParameters().stream())
	                    .filter(_c -> this.hasValidJobParameterPattern(_c.getAnnotation(Value.class)))
	                    .map(_c -> this.extractJobParameter(_c))
	                    .forEach(jobParameter -> jobParametersSet.add(jobParameter));
	        	});
	        
        	//System.out.println("─────────────────────────────────────────────────────────────────────────────────────");
        	//System.out.println("class:"+element.getQualifiedName().toString()+", beanNames:"+beanNames);
        	//System.out.println(jobName);
	        
	        if(!beanNames.contains(jobName)){
            	String errorMsg = element.getQualifiedName().toString()+"클래스에 @Bean(name=\""+jobName+"\")이 존재하지 않습니다.";
            	System.err.println(errorMsg);
            	throw new RuntimeException(errorMsg);
	        }
	        
	        TaskMetaObject.Schedule schedule = new TaskMetaObject.Schedule();
	        schedule.setScheduleType(annotationInfo.scheduleType());
	        schedule.setScheduleValue(annotationInfo.scheduleValue());

	        TaskMetaObject.Environment environment = new TaskMetaObject.Environment();
	        environment.setExecutionScript("@executionScript-"+jobName+"@");
	        environment.setPackagePath("@packagePath-"+jobName+"@");
	        environment.setRepositoryPath("@repositoryPath-"+jobName+"@");

	        TaskMetaObject taskMetaObject = new TaskMetaObject();	    
	        taskMetaObject.setProjectName(projectName);
	        taskMetaObject.setAppName(projectName+"-"+jobName);
	        taskMetaObject.setJobName(jobName);
	        taskMetaObject.setJobConfigClass(element.getQualifiedName().toString());
	        taskMetaObject.setJobDescription(annotationInfo.description());
	        taskMetaObject.setJobParameters(jobParametersSet);
	        taskMetaObject.setSchedule(schedule);
	        taskMetaObject.setEnvironment(environment);
	        
	        JsonObject result = new Gson().toJsonTree(taskMetaObject).getAsJsonObject();
	        
	        return result;
	    }

	    private String extractJobParameter(Element element) {
	        Value annotation = element.getAnnotation(Value.class);
	        if (annotation == null) return null;
	        String _value = annotation.value();  
	        return JOB_PARAM_PATTERN.matcher(_value).find()?_value.substring(_value.indexOf("[") + 1, _value.indexOf("]")):"";
	    }
	    
	    private boolean hasValidJobParameterPattern(Value annotation) {
	        return annotation != null && JOB_PARAM_PATTERN.matcher(annotation.value()).matches();
	    }

//	    private void writeJsonToMetaInf(String jsonOutput) {
//	        try {
//	            File metaInfDir = new File(this.getMetaInfPath());
//	            if (!metaInfDir.exists() && !metaInfDir.mkdirs()) throw new IOException("META-INF 디렉토리 생성 실패");
//
//	            try (Writer writer = new FileWriter(new File(metaInfDir, META_FILE))) {
//	                writer.write(jsonOutput);
//	            }
//	        } catch (IOException e) {
//	            e.printStackTrace();
//	        }
//	    }

	    private void writeJsonToMetaInf(String jsonOutput) {
	        try {
	            File metaInfDir = new File(this.getMetaInfPath());
	            if (!metaInfDir.exists() && !metaInfDir.mkdirs()) {
	                throw new IOException("META-INF 디렉토리 생성 실패");
	            }

	            File outputFile = new File(metaInfDir, META_FILE);
	            try (Writer writer = new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8)) {
	                writer.write(jsonOutput);
	            }
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }
	    
	    private String getMetaInfPath() throws IOException {
	        String classOutputPath = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", META_FILE).toUri().getPath();
	        File metaInfDir = new File(new File(classOutputPath).getParent(), "META-INF");

	        if (!metaInfDir.exists() && !metaInfDir.mkdirs()) {
	            throw new IOException("META-INF 디렉토리 생성 실패");
	        }

	        return metaInfDir.getAbsolutePath();
	    }

	    private String extractProjectName() {
	    	String projectName="";
	        try (FileReader fileReader = new FileReader("pom.xml")) {
	        	projectName = new MavenXpp3Reader().read(fileReader).getArtifactId();
	        } catch (FileNotFoundException e) {
	            System.err.println("pom.xml 파일을 찾을 수 없습니다.");
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
            return projectName;
	    }
    
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		
}