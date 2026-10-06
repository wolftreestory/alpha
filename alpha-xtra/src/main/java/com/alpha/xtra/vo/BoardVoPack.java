package com.alpha.xtra.vo;

import java.io.Serializable;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

public class BoardVoPack{

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	@Getter
	@Setter
	public static class BoardInDto {	
		private BoardVo board1;
		private BoardVo board2;
	}
	
	
	@Getter
	@Setter
	public static class BoardOutDto {		
		private List<BoardVo> list;
		private BoardVo board;
	}	
	
	@Getter
	@Setter
	@ToString
	public static class BoardVo implements Serializable {

		private static final long serialVersionUID = 1L;
	    
	    private int no;
	    private String title;
	    private String content;
	    private String name;
	    private String regDate;
	    private String modDate;
	    
		public BoardVo() {}
		public BoardVo(int no) {this.no = no;}
		
		@Builder
		public BoardVo(int no, String title, String content, String name, String regDate, String modDate) {
			super();
			this.no = no;
			this.title = title;
			this.content = content;
			this.name = name;
			this.regDate = regDate;
			this.modDate = modDate;
		}
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
}