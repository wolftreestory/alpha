package com.alpha.xtra.controller;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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
import com.alpha.base.advice.ExceptionAdvice;
import com.alpha.base.annotation.PageHandler;
import com.alpha.xtra.service.BoardService;
import com.alpha.xtra.vo.BoardVoPack.BoardVo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(path = "/xtra/redis/cache/v1")
@ConditionalOnProperty(name="spring.redis.enabled", havingValue="true", matchIfMissing=false)
public class RedisCacheController extends DefaultBinder {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final BoardService boardService;

	public RedisCacheController(BoardService boardService) {
		this.boardService = boardService;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@GetMapping("")
	@PageHandler
	@Cacheable(value = "com.alpha.xtra.controller.redisCacheController:boardList")
	@Operation(summary = "검색조회(페이징)", description = "[sample] 게시판 조회(검색,페이징)")
	public List<BoardVo> retrieveBoard(
			@Parameter(description = "번호") @RequestParam(required = false, defaultValue = "0") final Integer boardNo,
			@Parameter(description = "제목") @RequestParam(required = false, defaultValue = "") final String title,
			@Parameter(description = "내용") @RequestParam(required = false, defaultValue = "") final String content,
			@Parameter(description = "이름") @RequestParam(required = false, defaultValue = "") final String name,
			@Parameter(description = "페이지번호") @RequestParam(required = false) final String pageNo,
			@Parameter(description = "페이지크기") @RequestParam(required = false) final String rowSize) {

		log.debug(">> 검색조회(페이징)");

		BoardVo paramVo = BoardVo.builder().no(boardNo.intValue()).title(title).content(content).name(name).build();
		
		return this.boardService.getList(paramVo);
	}

	@GetMapping("/{boardNo}")
	@Cacheable(value = "com.alpha.xtra.controller.redisCacheController:board", key = "#boardNo")
	@Operation(summary = "조회", description = "[sample] 게시판 boardNo로 지정한 게시물 조회")
	public BoardVo retrieveBoard(@Parameter(description = "번호") @PathVariable final int boardNo) {

		log.debug(">> 조회");

		return this.boardService.get(boardNo);
	}
	
	@PostMapping("")
	@Caching(evict={@CacheEvict(value = "com.alpha.xtra.controller.redisCacheController:boardList", allEntries = true)},
			put={@CachePut(value = "com.alpha.xtra.controller.redisCacheController:board", key = "#boardNo")}
	)
	@Operation(summary = "생성", description = "[sample] 게시판 생성")
	public int createBoard(@Parameter(description = "게시판 Vo") @RequestBody BoardVo boardVo) {

		log.debug(">> 생성");

		Assert.notNull(boardVo.getTitle(), ExceptionAdvice.message("biz.error.004", "title"));
		Assert.notNull(boardVo.getContent(), ExceptionAdvice.message("biz.error.004", "content"));
		Assert.notNull(boardVo.getName(), ExceptionAdvice.message("biz.error.004", "name"));

		int boardNo = this.boardService.create(boardVo);

		log.debug(">> 생성번호:{}", boardNo);

		return boardNo;
	}

	@PutMapping("/{boardNo}")
	@Caching(evict={
			@CacheEvict(value = "com.alpha.xtra.controller.redisCacheController:board", key = "#boardNo"),
			@CacheEvict(value = "com.alpha.xtra.controller.redisCacheController:boardList", allEntries = true)
			}
	)
	@Operation(summary = "수정", description = "[sample] 게시판 boardNo로 지정한 게시물 수정")
	public int updateBoard(@Parameter(description = "번호") @PathVariable final int boardNo,
			@Parameter(description = "게시판 Vo") @RequestBody BoardVo boardVo) {

		log.debug(">> 수정");

		boardVo.setNo(boardNo);
		int count = this.boardService.update(boardVo);

		log.debug(">> 수정건수:{}", count);

		return count;
	}

	@DeleteMapping("/{boardNo}")
	@Caching(evict={
			@CacheEvict(value = "com.alpha.xtra.controller.redisCacheController:board", key = "#boardNo"),
			@CacheEvict(value = "com.alpha.xtra.controller.redisCacheController:boardList", allEntries = true)
			}
	)
	@Operation(summary = "삭제", description = "[sample] 게시판 boardNo로 지정한 게시물 삭제")
	public int deleteBoard(@Parameter(description = "번호") @PathVariable final int boardNo) {

		log.debug(">> 삭제");

		int count = this.boardService.delete(boardNo);

		log.debug(">> 삭제건수:{}", count);

		return count;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}
