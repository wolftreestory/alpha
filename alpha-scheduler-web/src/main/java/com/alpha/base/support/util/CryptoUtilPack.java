package com.alpha.base.support.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.tomcat.util.codec.binary.Base64;
import org.jasypt.encryption.StringEncryptor;

import com.alpha.base.config.JasyptConfig;
import com.alpha.base.support.aid.BeanAidPack.BeanAid;

import lombok.Builder;
import lombok.Data;

public final class CryptoUtilPack {
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public interface CryptoUtil {
		public static final String DEFAULT_AES256_KEY="alpha.123456789";		
		public AES256 getAES256();
		public SHA256 getSHA256();
		public SHA512 getSHA512();		
		public StringEncryptor getStringEncryptor();

	}
	
	public interface AES256 {
		public String encryption(String data);
		public String encryption(String data,String key);
		public String decryption(String data);
		public String decryption(String data,String key);
	}
	
	public interface SHA256 {
		public String encryption(String data);
		public boolean isMatch(String encryption,String data);
	}
	
	public interface SHA512 {
	    public String encryption(String data);
		public boolean isMatch(String encryption,String data);
		
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Data @Builder
	public static class Meta {
		private BeanAid beanAid;
	}
	
	public static CryptoUtil getCryptoUtil(Meta meta) {
		
		return new CryptoUtil() {
			@Override
			public SHA256 getSHA256() {
				return new SHA256() {
					@Override
					public String encryption(String data) {
						String result=null;
						try {
							MessageDigest digest=MessageDigest.getInstance("SHA-256");
							digest.reset();
							digest.update(data.getBytes("utf8"));
							result=String.format("%064x",new BigInteger(1,digest.digest()));
						}catch(Exception e) {
							throw new RuntimeException(e.getMessage());
						}
						return result;
					}
					
					@Override
					public boolean isMatch(String encryption,String data) {
						if(this.encryption(data).equals(encryption)) {return true;}
						return false;
					}		
				};
			}

			@Override
			public SHA512 getSHA512() {
				return new SHA512() {
					@Override
					public String encryption(String data) {
				        String result=null;
				        try {
				            MessageDigest digest=MessageDigest.getInstance("SHA-512");
				            digest.reset();
				            digest.update(data.getBytes("utf8"));
				            result=String.format("%0128x",new BigInteger(1,digest.digest()));
				        }catch(Exception e) {
				            throw new RuntimeException(e.getMessage());
				        }
				        return result;
				    }
					
					@Override
					public boolean isMatch(String encryption,String data) {
						if(this.encryption(data).equals(encryption)) {return true;}
						return false;
					}		
				};
			}
			
			@Override
			public AES256 getAES256(){
				return new AES256() {
					@Override
					public String encryption(String data) {
						return this.encryption(data, DEFAULT_AES256_KEY);
					}
	
					@Override
					public String encryption(String data,String key) {
						String encStr=null;
						try {	
							Cipher cipher=Cipher.getInstance("AES/CBC/PKCS5Padding");
							cipher.init(Cipher.ENCRYPT_MODE,this.getSecretKeySpec(key),this.getIvParameterSpec(key));
							byte[] encBytes=cipher.doFinal(data.getBytes("UTF-8"));
							encStr=Base64.encodeBase64String(encBytes);
						}catch(Exception e) {	
							throw new RuntimeException(e.getMessage());
						}
						return encStr;
					}	
					
					@Override
					public String decryption(String data) {
						return this.decryption(data, DEFAULT_AES256_KEY);	
					}
					
					@Override
					public String decryption(String data,String key) {
						String decStr=null;		
						try {	
							Cipher cipher=Cipher.getInstance("AES/CBC/PKCS5Padding");
							cipher.init(Cipher.DECRYPT_MODE,this.getSecretKeySpec(key),this.getIvParameterSpec(key));
							byte[] decBytes=Base64.decodeBase64(data.getBytes("UTF-8"));
							decStr=new String(cipher.doFinal(decBytes),"UTF-8");
						}catch(Exception e) {
							throw new RuntimeException(e.getMessage());
						}
						return decStr;
					}	
					
					private SecretKeySpec getSecretKeySpec(String key) {
						SecretKeySpec secretKeySpec=null;
						try {
							SecureRandom secureRandom=SecureRandom.getInstance("SHA1PRNG");
							secureRandom.setSeed(key.getBytes("UTF-8"));
							
							KeyGenerator keyGenerator=KeyGenerator.getInstance("AES");
							keyGenerator.init(256,secureRandom);
							
							SecretKey secureKey=keyGenerator.generateKey();
							secretKeySpec=new SecretKeySpec(secureKey.getEncoded(),"AES");
						}catch(Exception e) {
							throw new RuntimeException(e.getMessage());
						}
						return secretKeySpec;
					}
					
					private IvParameterSpec getIvParameterSpec(String key) {
						IvParameterSpec ivParameterSpec=null;
						try {
							String ivTemplateStr="beethoven.egmont.overture"; //반드시 16자 이상
							byte[] ivTemplateBytes=ivTemplateStr.getBytes("UTF-8");
					
							byte[] keyBytes=key.getBytes("UTF-8");
							int max=(keyBytes.length<17?keyBytes.length:16);
							for(int i=0;i<max;i++) {ivTemplateBytes[i]=keyBytes[i];}
					
							byte[] ivParamBytes=Arrays.copyOfRange(ivTemplateBytes,0,16); 
							ivParameterSpec=new IvParameterSpec(ivParamBytes);
						}catch(Exception e) {
							throw new RuntimeException(e.getMessage());
						}
						return ivParameterSpec;
					}
				};
			}
		
			@Override			
			public StringEncryptor getStringEncryptor() {
				return meta.getBeanAid().getBean(JasyptConfig.STRING_ENCRYPTOR,StringEncryptor.class);
			}
		};
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}