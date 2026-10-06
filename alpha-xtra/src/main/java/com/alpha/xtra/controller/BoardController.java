package com.alpha.xtra.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alpha.Application.DefaultBinder;
import com.alpha.base.BaseUtil;
import com.alpha.base.advice.ExceptionAdvice;
import com.alpha.base.annotation.MaskHandler;
import com.alpha.base.annotation.PageHandler;
import com.alpha.base.exception.BusinessException;
import com.alpha.xtra.service.BoardService;
import com.alpha.xtra.vo.BoardVoPack.BoardInDto;
import com.alpha.xtra.vo.BoardVoPack.BoardOutDto;
import com.alpha.xtra.vo.BoardVoPack.BoardVo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(path = "/xtra/boards/v1")
public class BoardController extends DefaultBinder {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private final BoardService boardService;

	public BoardController(BoardService boardService) {
		this.boardService = boardService;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@GetMapping("")
	@PageHandler
	@MaskHandler("mask.biz.board.list")
	@Operation(summary = "검색조회(페이징)", description = "[sample] 게시판 조회(검색,페이징)")
	public List<BoardVo> retrieveBoard(
			@Parameter(description = "번호") @RequestParam(required = false, defaultValue = "0") final Integer no,
			@Parameter(description = "제목") @RequestParam(required = false, defaultValue = "") final String title,
			@Parameter(description = "내용") @RequestParam(required = false, defaultValue = "") final String content,
			@Parameter(description = "이름") @RequestParam(required = false, defaultValue = "") final String name,
			@Parameter(description = "페이지번호") @RequestParam(required = false) final String pageNo,
			@Parameter(description = "페이지크기") @RequestParam(required = false) final String rowSize) {

		log.debug(">> 검색조회(페이징)");
		log.debug(">> isMaskApply:{}",BaseUtil.getMaskContext().isMaskApply());

		BoardVo paramVo = BoardVo.builder()
				.no(no.intValue())
				.title(title)
				.content(content)
				.name(name)
				.build();

		return this.boardService.getList(paramVo);
	}

	@PostMapping("/:collection")
	@PageHandler
	@Operation(summary = "멀티조회(페이징)", description = "[sample] 게시판 BoardNo를 List로 여러개 지정하여 게시물 페이징 조회")
	public List<BoardVo> retrieveBoard(
			@Parameter(description = "번호목록") @RequestBody final List<Integer> foreach,
			@Parameter(description = "페이지번호") @RequestParam(required = false) final String pageNo,
			@Parameter(description = "페이지크기") @RequestParam(required = false) final String rowSize) {

		log.debug(">> 멀티조회(페이징)");

		return this.boardService.getList(foreach);
	}

	@GetMapping("/{boardNo}")
	@Operation(summary = "조회", description = "[sample] 게시판 boardNo로 지정한 게시물 조회")
	public BoardVo retrieveBoard(
			@Parameter(description = "번호") @PathVariable final int boardNo) {

		log.debug(">> 조회");

		return this.boardService.get(boardNo);
	}

	@GetMapping("/{boardNo}:complex")
	@PageHandler
	@Operation(summary = "복합조회(페이징)", description = "[sample] 게시판 boardNo로 지정한 게시물과 게시판 BoardNo를 List로 여러개 지정하여 게시물 페이징 조회")
	public BoardOutDto retrieveBoard(
			@Parameter(description = "번호") @PathVariable final int boardNo,
			@Parameter(description = "번호들") @RequestParam String idxs,
			@Parameter(description = "페이지번호") @RequestParam(required = false) final String pageNo,
			@Parameter(description = "페이지크기") @RequestParam(required = false) final String rowSize) {

		log.debug(">> 복합조회(페이징)");

		List<?> list = StringUtils.isBlank(idxs)?null:Arrays.asList(idxs.split(","));
		if (list == null) {
			throw new BusinessException(ExceptionAdvice.message("sys.valid.001"), HttpStatus.CONFLICT);
		}

		BoardOutDto out = new BoardOutDto();
		out.setBoard(this.boardService.get(boardNo));
		out.setList(this.boardService.getList(list));

		return out;
	}

	@PostMapping("")
	@Operation(summary = "생성", description = "[sample] 게시판 생성")
	public int createBoard(
			@Parameter(description = "게시판 Vo") @RequestBody BoardVo boardVo) {

		log.debug(">> 생성");

		Assert.notNull(boardVo.getTitle(), ExceptionAdvice.message("biz.error.004", "title"));
		Assert.notNull(boardVo.getContent(), ExceptionAdvice.message("biz.error.004", "content"));
		Assert.notNull(boardVo.getName(), ExceptionAdvice.message("biz.error.004", "name"));
		
		int boardNo = this.boardService.create(boardVo);

		log.debug(">> 생성번호:{}", boardNo);

		return boardNo;
	}

	@PutMapping("/{boardNo}")
	@Operation(summary = "수정", description = "[sample] 게시판 boardNo로 지정한 게시물 수정")
	public int updateBoard(
			@Parameter(description = "번호") @PathVariable final int boardNo,
			@Parameter(description = "게시판 Vo") @RequestBody BoardVo boardVo) {

		log.debug(">> 수정");

		boardVo.setNo(boardNo);
		int count = this.boardService.update(boardVo);

		log.debug(">> 수정건수:{}", count);

		return count;
	}

	@DeleteMapping("/{boardNo}")
	@Operation(summary = "삭제", description = "[sample] 게시판 boardNo로 지정한 게시물 삭제")
	public int deleteBoard(
			@Parameter(description = "번호") @PathVariable final int boardNo) {

		log.debug(">> 삭제");

		int count = this.boardService.delete(boardNo);

		log.debug(">> 삭제건수:{}", count);

		return count;
	}

	@PostMapping("/:complex")
	@Operation(summary = "복합처리", description = "[sample] 게시판 다중처리 후 생성PK조회")
	public List<?> upsertBoard(
			@Parameter(description = "게시판 upsert") @RequestBody final BoardInDto inDto) {

		log.debug(">> 다중처리");

		List<BoardVo> list = new ArrayList<>();
		list.add(inDto.getBoard1());
		list.add(inDto.getBoard2());

		List<?> result = this.boardService.upsert(list);

		log.debug(">> 다중처리:{}", result.toString());

		return result;
	}

    @GetMapping("/json2FileDownload")
    public void string2FileDownload(
    		@Parameter(description = "파일이름") @RequestParam(required = false, defaultValue = "testFile") final String fileName,
    		@Parameter(description = "데이타반복") @RequestParam(required = true, defaultValue = "1") final int count,
    		HttpServletRequest request,HttpServletResponse response) {
		
    	log.debug(">> json2FileDownload");

		List<BoardVo> list = this.boardService.getList(new BoardVo());
		String data = BaseUtil.getJsonUtil().toJson(list)+"\n";
		try {
			if(count==1) {
				BaseUtil.getFileUtil().pushout(data.getBytes(),fileName,request,response);
			} else {
				BaseUtil.getFileUtil().pushout((i)->{return i<=count?data.getBytes():null;}, fileName, request, response);
			}
		} catch (Exception e) {
			throw new BusinessException(()->ExceptionAdvice.message("biz.board.005"),HttpStatus.CONFLICT);
		}
    }    

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}
