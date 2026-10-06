package com.alpha.batch.domain.sample.board.vo;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BoardVo implements Serializable {
	private static final long serialVersionUID = 1L;

	private int no;
	private String title;
	private String content;
	private String name;
	private String regDate;
	private String modDate;

	public BoardVo(int no) {
		this.no = no;
	}
}