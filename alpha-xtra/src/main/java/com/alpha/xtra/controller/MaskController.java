package com.alpha.xtra.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alpha.Application.DefaultBinder;
import com.alpha.base.BaseUtil;
import com.alpha.base.annotation.MaskHandler;
import com.alpha.xtra.service.BoardService;
import com.alpha.xtra.vo.BoardVoPack.BoardVo;

import io.swagger.v3.oas.annotations.Parameter;

//@Slf4j
@RestController
@RequestMapping(path="/xtra/mask/v1")
@ConditionalOnExpression("'${alpha.mask.enabled:false}'.equals('true')")
public class MaskController extends DefaultBinder {

	private final BoardService boardService;

	public MaskController(BoardService boardService) {
		this.boardService = boardService;
	}

    @GetMapping("/:doMask")
    public Map<String,Object> doMask(
		@Parameter(description="이름 [한글<5:이름의 첫자리 마스킹,한글>4:앞뒤2자리 이외 마스킹, 영문>5:앞뒤2자리 이외 마스킹, 영문<6:중간절반마스킹]") @RequestParam(required=false, defaultValue="홍길동") final String name,
		@Parameter(description="주민등록번호 [내국인, 외국인]") @RequestParam(required=false, defaultValue="850717-1023311") final String juminNo,
		@Parameter(description="운전면허 [구번호:지역이름으로 시작, 신규번호:숫자로 시작]") @RequestParam(required=false, defaultValue="22-11-123456-90") final String driverLicense,		
		@Parameter(description="여권 [뒤 4자리 이외 마스킹]") @RequestParam(required=false, defaultValue="M12345678") final String passPort,
		@Parameter(description="전화 [모바일, 지역, 국제:+로시작]") @RequestParam(required=false, defaultValue="010-1234-5678") final String phone,
		@Parameter(description="이메일 [@앞 영문이름규칙을 따름]") @RequestParam(required=false, defaultValue="hong@gmail.co.kr") final String email,
		@Parameter(description="ip [ipv4:3블럭 마스킹, ipv6:뒤2블럭 마스킹]") @RequestParam(required=false, defaultValue="0.0.0.0") final String ip,
		@Parameter(description="주소 [숫자만 마스킹]") @RequestParam(required=false, defaultValue="서울 종로구 사직로 161(세종로, 경복궁)") final String address,
		@Parameter(description="신용카드 [앞3자리, 뒤4자리 이외 마스킹]") @RequestParam(required=false, defaultValue="4321-5678-9876-1092") final String creditCard,
		@Parameter(description="은행계좌번호 [앞3자리, 뒤4자리 이외 마스킹]") @RequestParam(required=false, defaultValue="123123456789") final String bankAccount,
		@Parameter(description="금액 [뒤3자리 이외 마스킹]") @RequestParam(required=false, defaultValue="100000000") final String money){

		Map<String,Object> map = new HashMap<>();
		if(!StringUtils.isBlank(name)){map.put("name",BaseUtil.getMaskUtil().maskName(name));}
		if(!StringUtils.isBlank(juminNo)){map.put("juminNo",BaseUtil.getMaskUtil().maskJuminNo(juminNo));}
		if(!StringUtils.isBlank(driverLicense)){map.put("driverLicense",BaseUtil.getMaskUtil().maskDriverLicense(driverLicense));}
		if(!StringUtils.isBlank(passPort)){map.put("passPort",BaseUtil.getMaskUtil().maskPassPort(passPort));}	
		if(!StringUtils.isBlank(phone)){map.put("phone",BaseUtil.getMaskUtil().maskPhone(phone));}
		if(!StringUtils.isBlank(email)){map.put("email",BaseUtil.getMaskUtil().maskEmail(email));}
		if(!StringUtils.isBlank(ip)){map.put("ip",BaseUtil.getMaskUtil().maskIp(ip));}
		if(!StringUtils.isBlank(address)){map.put("address",BaseUtil.getMaskUtil().maskAddress(address));}
		if(!StringUtils.isBlank(creditCard)){map.put("creditCard",BaseUtil.getMaskUtil().maskCreditCard(creditCard));}
		if(!StringUtils.isBlank(bankAccount)){map.put("bankAccount",BaseUtil.getMaskUtil().maskBankAccount(bankAccount));}
		if(!StringUtils.isBlank(money)){map.put("money",BaseUtil.getMaskUtil().maskMoney(money));}

        return map;
    }

	@GetMapping(":doMaskItem")
	@MaskHandler("mask.biz.board.item1")
	public BoardVo doMaskItem() {		
		List<BoardVo> list = this.boardService.getList(new BoardVo());
		return list.get(0);
	}	
	
	@GetMapping(":doMaskList")
	@MaskHandler(value={"mask.biz.board.item","mask.biz.board.list"})
	public List<BoardVo> doMaskList() {
		return this.boardService.getList(new BoardVo());
	}
}
