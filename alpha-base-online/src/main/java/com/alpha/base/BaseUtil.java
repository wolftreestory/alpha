package com.alpha.base;

import com.alpha.base.support.AbstractMetaOnline;

public final class BaseUtil extends AbstractMetaOnline {} 

interface DataSourceConfig{public static boolean isEnabled() {return true;}}
interface JasyptConfig{public static boolean isEnabled() {return true;}}
interface JwtConfig{public static boolean isEnabled() {return false;}}
interface KafkaConfig{public static boolean isEnabled() {return false;}}
interface MessageSourceConfig{public static boolean isEnabled() {return true;}}
interface MybatisConfig{public static boolean isEnabled() {return true;}}
interface PropertiesConfig{public static boolean isEnabled() {return true;}}
interface RedisConfig{public static boolean isEnabled() {return false;}}
interface SecurityConfig{public static boolean isEnabled() {return false;}}
interface SchedulerConfig{public static boolean isEnabled() {return true;}}
interface SwaggerConfig{public static boolean isEnabled() {return true;}}
interface TomcatClusterConfig{public static boolean isEnabled() {return false;}}
interface TransactionConfig{public static boolean isEnabled() {return true;}}
interface WebMvcConfig{public static boolean isEnabled() {return true;}}

interface MaskContext{public static boolean isEnabled() {return true;}}
interface PageContext{public static boolean isEnabled() {return true;}}
