package com.alpha.ytra.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alpha.ytra.service.BoardService;
import com.alpha.ytra.vo.BoardVoPack.BoardVo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(path="/ytra/etc/v1")
@ConditionalOnExpression("T(com.alpha.base.KafkaConfig).isEnabled()")
public class PublishController {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Value("${spring.servlet.multipart.location}")
	private String tempLocation;
	
	private final BoardService boardService;

	public PublishController(BoardService boardService) {
		this.boardService = boardService;
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@PostMapping("/publishData")
	@Operation(summary = "kafka publish", description = "지정한 갯수,발생주기에 의해 데이타를(BoardVo Object) kafka서버로 발행")   
	public void publishData(
			@Parameter(description = "토픽이름") @RequestParam(required = false, defaultValue = "testBatchTopic") final String topicName
			,@Parameter(description = "갯수") @RequestParam(required = false, defaultValue = "1") final Integer number
			,@Parameter(description = "발생주기(ms)") @RequestParam(required = false, defaultValue = "0") final Integer interval
			) {

		for(int i=0;i<number;i++) {
			this.threadSleepRandom(interval,0);
			this.boardService.publishData(topicName, BoardVo.builder()
					.title("publishBoardData_"+i+"_"+System.currentTimeMillis())
					.content("kafka 테스트 자동발생 데이타")
					.name("테스터")
					.build());
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void threadSleepRandom(long base,long scale) {
    	if(base<=0) {return;}
		try {
			long millis = base+(long)(Math.random()*scale);
			log.debug(">> sleep:{}",millis);
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}	
    }

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}
