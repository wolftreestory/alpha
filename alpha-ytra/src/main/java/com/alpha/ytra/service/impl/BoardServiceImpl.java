package com.alpha.ytra.service.impl;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import com.alpha.base.BaseUtil;
import com.alpha.base.context.PageContext;
import com.alpha.ytra.publisher.KafkaBoardPublisher1;
import com.alpha.ytra.publisher.KafkaBoardPublisher2;
import com.alpha.ytra.service.BoardService;
import com.alpha.ytra.vo.BoardVoPack.BoardInDto;
import com.alpha.ytra.vo.BoardVoPack.BoardVo;
import com.fasterxml.jackson.core.type.TypeReference;

@Service
public class BoardServiceImpl implements BoardService {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final String XTRA_BASE_URI = "http://localhost:9970";
	
    private final KafkaBoardPublisher1 boardPublisher1;

    private final KafkaBoardPublisher2 boardPublisher2;

	public BoardServiceImpl(KafkaBoardPublisher1 boardPublisher1, KafkaBoardPublisher2 boardPublisher2) {
		this.boardPublisher1 = boardPublisher1;
		this.boardPublisher2 = boardPublisher2;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
    public void publishData(String topicName, BoardVo vo) {
		this.publish(topicName,"create",vo);
    }
	
	@Override
	public List<BoardVo> getList(BoardVo vo) {
		if(vo==null) {return null;}
		
		Integer no = vo.getNo();
		String title = vo.getTitle();
		String content = vo .getContent();
		String name = vo.getName();
		String pageNo = String.valueOf(BaseUtil.getPageContext().getPageNo());
		String rowSize = String.valueOf(BaseUtil.getPageContext().getRowSize());
		
		URI apiUri = UriComponentsBuilder.fromUriString(XTRA_BASE_URI)
				.path("/xtra/boards/v1/")
				.queryParam("no", no)
				.queryParam("title", title)
				.queryParam("content", content)
				.queryParam("name", name)
				.queryParam("pageNo", pageNo)
				.queryParam("rowSize", rowSize)
				.encode().build().toUri();
		
		// GET 메서드를 이용해 요청한 URL로부터 ResponseEntity로 응답,  ResponseEntity 표준은 String이며, Jackson을 이용해 파싱해 사용.
		ResponseEntity<Object> responseEntity = BaseUtil.getRestTemplateUtil().getForEntity(apiUri, Object.class);
		
		List<BoardVo> result = null;
		if(responseEntity!=null) {
			PageContext responsePageContext = BaseUtil.getPageContext(responseEntity.getHeaders());
			BaseUtil.getPageContext().setTotalCount(responsePageContext.getTotalCount());
			result = BaseUtil.getObjectMapperUtil().convert(responseEntity.getBody(), new TypeReference<List<BoardVo>>() {});
		}

		return result;
	}
	
	@Override
	public List<BoardVo> getList(List<?> foreach){
		if(foreach==null) {return null;}
		
		URI apiUri = UriComponentsBuilder.fromUriString(XTRA_BASE_URI)
				.path("/xtra/boards/v1/:collection")
				.queryParam("pageNo", String.valueOf(BaseUtil.getPageContext().getPageNo()))
				.queryParam("rowSize", String.valueOf(BaseUtil.getPageContext().getRowSize()))
				.encode().build().toUri();
		
		//POST 메서드를 이용해 URL로 생성할 리소스를 보내고 객체로 응답.
		Object responseObject = BaseUtil.getRestTemplateUtil().postForObject(apiUri, foreach, Object.class);
		
		List<BoardVo> result = null;
		if(responseObject!=null) {
			result = BaseUtil.getObjectMapperUtil().convert(responseObject, new TypeReference<List<BoardVo>>(){});
		}

		return result;
	}
	    
	@Override
	public BoardVo get(int boardNo) {
		URI apiUri = UriComponentsBuilder.fromUriString(XTRA_BASE_URI)
				.path("/xtra/boards/v1/"+String.valueOf(boardNo))
				.queryParam("boardNo", String.valueOf(boardNo))
				.encode().build().toUri();
		
		// GET 메서드를 이용해 요청한 URL로부터 객체로 응답.
		BoardVo result = BaseUtil.getRestTemplateUtil().getForObject(apiUri, BoardVo.class);
		
		return result;
	}

	@Override
	public int create(BoardVo vo, InterfaceType sendType) {	
		if(sendType.equals(InterfaceType.kafka)) {
			this.publish("create",vo);
			return 1;
		}

		URI apiUri = UriComponentsBuilder.fromUriString(XTRA_BASE_URI)
				.path("/xtra/boards/v1")
				.encode().build().toUri();
		
		// POST 메서드를 이용해 URL로 생성할 리소스를 보내고 객체로 응답.
		Integer result = BaseUtil.getRestTemplateUtil().postForObject(apiUri, vo, Integer.class);
		
		return result;
	}

	@Override
	public int update(BoardVo vo, InterfaceType sendType) {		
		if(sendType.equals(InterfaceType.kafka)) {
			this.publish("update",vo);
			return vo.getNo();
		}	

		URI apiUri = UriComponentsBuilder.fromUriString(XTRA_BASE_URI)
				.path("/xtra/boards/v1/"+String.valueOf(vo.getNo()))
				.encode().build().toUri();	
		
		// URL로 PUT 메서드를 요청
		BaseUtil.getRestTemplateUtil().put(apiUri, vo);
		
		return vo.getNo();
	}
	
	@Override
	public int delete(int boardNo, InterfaceType sendType) {		
		if(sendType.equals(InterfaceType.kafka)) {
			this.publish("delete",new BoardVo(boardNo));
			return 1;
		}			

		URI apiUri = UriComponentsBuilder.fromUriString(XTRA_BASE_URI)
				.path("/xtra/boards/v1/"+String.valueOf(boardNo))
				.encode().build().toUri();	
		
		// URL로 DELETE 메서드를 요청.
		BaseUtil.getRestTemplateUtil().delete(apiUri);
		
		return 1;
	}
	
	@Override
	public List<?> upsert(List<BoardVo> list, InterfaceType sendType) {
		BoardInDto inDto = new BoardInDto();
		inDto.setBoard1(list.get(0));
		inDto.setBoard2(list.get(1));

		if(sendType.equals(InterfaceType.kafka)) {
			this.publish("upsert",inDto);
			return null;		
		}

		URI apiUri = UriComponentsBuilder.fromUriString(XTRA_BASE_URI)
				.path("/xtra/boards/v1/:complex")
				.encode().build().toUri();	
		
		// POST 메서드를 이용해 URL로 생성할 리소스를 보내고 객체로 응답.
		List<?> result = BaseUtil.getRestTemplateUtil().postForObject(apiUri,inDto,List.class);
		
		return result;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void publish(String method, BoardVo vo) {
    	this.publish("testTopic", method, vo);
	}

    private void publish(String topic, String method, BoardVo vo) {
    	Map<String, String> header = BaseUtil.getKafkaUtil().buildHeader();
    	header.put("method", method);
    	header.put("header-key1", "value1");
    	header.put("header-key2", "value2");

		this.boardPublisher1.send(topic,"kafka-key1",vo,header);
		//this.boardPublisher1.send(topic,"kafka-key1",BaseUtil.getKafkaUtil().toJson(vo),header);
	}
    
    private void publish(String method, BoardInDto dto) {
    	this.publish("testTopic", method, dto);
	}

    private void publish(String topic, String method, BoardInDto dto) {
    	Map<String, String> header = BaseUtil.getKafkaUtil().buildHeader();
    	header.put("method", method);
    	header.put("header-key1", "value1");
    	header.put("header-key2", "value2");

		this.boardPublisher2.send(topic,"kafka-key1",dto,header);
		//this.boardPublisher2.send(topic,"kafka-key1",BaseUtil.getKafkaUtil().toJson(dto),header);
	}
    
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
}
