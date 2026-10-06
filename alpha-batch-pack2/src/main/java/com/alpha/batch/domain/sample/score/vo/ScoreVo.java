package com.alpha.batch.domain.sample.score.vo;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScoreVo implements Serializable {
	private static final long serialVersionUID = 1L;

	private int no;
	private String name;
	private long score;

	public ScoreVo(int no) {
		this.no = no;
	}
}