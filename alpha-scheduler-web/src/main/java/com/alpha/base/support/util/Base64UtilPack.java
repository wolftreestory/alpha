package com.alpha.base.support.util;

import org.apache.tomcat.util.codec.binary.Base64;

public final class Base64UtilPack {
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public interface Base64Util {
		public byte[] getEncodeBase64(String str);
		public byte[] getEncodeBase64(byte[] bytes);
		
		public byte[] getDecodeBase64(String str);
		public byte[] getDecodeBase64(byte[] bytes);
		
		public String getEncodeBase64String(String str);
		public String getEncodeBase64String(byte[] bytes);

		public String getDecodeBase64String(String str);
		public String getDecodeBase64String(byte[] bytes);
		
		public boolean isBase64(String str);
		public boolean isBase64(byte[] bytes);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static Base64Util getBase64Util() {
		
		return new Base64Util() {

			@Override
			public byte[] getEncodeBase64(String str) {
				return (str==null?null:this.getEncodeBase64(str.getBytes()));
			}
			
			@Override
			public byte[] getEncodeBase64(byte[] bytes) {
				return (bytes==null?null:Base64.encodeBase64(bytes));
			}

			@Override
			public byte[] getDecodeBase64(String str) {
				return (str==null?null:this.getDecodeBase64(str.getBytes()));
			}
			
			@Override
			public byte[] getDecodeBase64(byte[] bytes) {
				return (bytes==null?null:Base64.decodeBase64(bytes));
			}
			
			@Override
			public String getEncodeBase64String(String str) {
				return (str==null?null:new String(this.getEncodeBase64(str)));
			}
			
			@Override
			public String getEncodeBase64String(byte[] bytes) {
				return (bytes==null?null:new String(this.getEncodeBase64(bytes)));
			}

			@Override
			public String getDecodeBase64String(String str) {
				return (str==null?null:new String(this.getDecodeBase64(str)));
			}
			
			@Override
			public String getDecodeBase64String(byte[] bytes) {
				return (bytes==null?null:new String(this.getDecodeBase64(bytes)));
			}
			
			public boolean isBase64(String str) {
				return Base64.isBase64(str);
			}
			
			public boolean isBase64(byte[] bytes) {
				return Base64.isBase64(bytes);				
			}
			
		};
	}
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}
