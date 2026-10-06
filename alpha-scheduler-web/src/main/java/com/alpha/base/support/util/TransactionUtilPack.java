package com.alpha.base.support.util;

import java.util.UUID;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class TransactionUtilPack {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public interface TransactionUtil {

	    public TransactionStatus start() throws TransactionException;	  
	    public TransactionStatus start(int propagationBehavior) throws TransactionException;
	    public void commit(TransactionStatus txStatus) throws TransactionException;
	    public void rollback(TransactionStatus txStatus) throws TransactionException;
	    public void end(TransactionStatus txStatus) throws TransactionException;

	    public String getCurrentTransactionName();
	    public boolean isLookup(String jndiName);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static TransactionUtil getTransactionUtil(PlatformTransactionManager txManager) {

		return new TransactionUtil() {
	    	// TransactionDefinition.PROPAGATION_REQUIRED : 부모 트랜잭션 내에서 실행하며 부모 트랜잭션이 없을 경우 새로운 트랜잭션을 생성
	    	// TransactionDefinition.PROPAGATION_REQUIRES_NEW : 부모 트랜잭션을 무시하고 무조건 새로운 트랜잭션이 생성
	    	// TransactionDefinition.PROPAGATION_SUPPORT : 부모 트랜잭션 내에서 실행하며 부모 트랜잭션이 없을 경우 nontransactionally로 실행
	    	// TransactionDefinition.PROPAGATION_MANDATORY : 부모 트랜잭션 내에서 실행되며 부모 트랜잭션이 없을 경우 예외가 발생
	    	// TransactionDefinition.PROPAGATION_NOT_SUPPORT : nontransactionally로 실행하며 부모 트랜잭션 내에서 실행될 경우 일시 정지
	    	// TransactionDefinition.PROPAGATION_NEVER : nontransactionally로 실행되며 부모 트랜잭션이 존재한다면 예외가 발생
	    	// TransactionDefinition.PROPAGATION_NESTED : 해당 메서드가 부모 트랜잭션에서 진행될 경우 별개로 커밋되거나 롤백될 수 있음. 둘러싼 트랜잭션이 없을 경우 REQUIRED와 동일하게 작동
			
		    @Override
		    public TransactionStatus start() throws TransactionException {		    	
		        return this.start(TransactionDefinition.PROPAGATION_REQUIRED);
		    }

		    @Override
		    public TransactionStatus start(int propagationBehavior) throws TransactionException {
		        return this.start(txManager, propagationBehavior);
		    }

		    @Override
		    public void commit(TransactionStatus txStatus) throws TransactionException {
		        if(txStatus!=null && !txStatus.isCompleted()) {
		            //log.debug(">> transaction commit");
		            txManager.commit(txStatus);
		        }
		    }

		    @Override
		    public void rollback(TransactionStatus txStatus) throws TransactionException {
		        if(txStatus!=null && !txStatus.isCompleted()) {
		            //log.debug(">> transaction rollback");
		            txManager.rollback(txStatus);
		        }
		    }

		    @Override
		    public void end(TransactionStatus txStatus) throws TransactionException {
		        //log.debug(">> transaction end");        
		        txStatus=null;
		    }

		    @Override
		    public String getCurrentTransactionName() {
		    	return TransactionSynchronizationManager.getCurrentTransactionName();
		    }

		    @Override
			public boolean isLookup(String jndiName) {
				boolean isEnable=false;
				Context ctx = null;
				try {
					ctx = new InitialContext();
					if(ctx.lookup(jndiName)!=null) {isEnable=true;}
				} catch (NamingException e) {
					throw new RuntimeException(e);
				} finally {
					if(ctx!=null) {try {ctx.close();} catch (Exception e) {log.error(">> {}",e.getMessage());}}
				}
				return isEnable;
			}   
		    
		    private TransactionStatus start(PlatformTransactionManager txManager,int propagationBehavior) throws TransactionException {
		    	if(txManager==null) {return null;}
	            //log.debug(">> transaction start");
		        DefaultTransactionDefinition txDef=new DefaultTransactionDefinition();
		        txDef.setPropagationBehavior(propagationBehavior);
		        txDef.setName("tx"+System.currentTimeMillis()+"#"+UUID.randomUUID().toString());
		        return txManager.getTransaction(txDef);
		    }
		    
		};
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}
