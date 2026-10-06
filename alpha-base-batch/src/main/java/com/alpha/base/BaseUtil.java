package com.alpha.base;

import com.alpha.base.support.AbstractMetaBatch;

public class BaseUtil extends AbstractMetaBatch {}

interface DatasourceConfig{public static boolean isEnabled() {return false;}} //only online
interface JasyptConfig{public static boolean isEnabled() {return true;}}
interface KafkaConfig{public static boolean isEnabled() {return false;}}
interface MessageSourceConfig{public static boolean isEnabled() {return true;}}
interface MybatisConfig{public static boolean isEnabled() {return false;}} //only online
interface PropertiesConfig{public static boolean isEnabled() {return true;}}
interface SchedulerConfig{public static boolean isEnabled() {return false;}} //only online
interface SwaggerConfig{public static boolean isEnabled() {return false;}} //only online
interface TomcatClusterConfig{public static boolean isEnabled() {return false;}} //only online
interface TransactionConfig{public static boolean isEnabled() {return false;}} //only online
interface WebMvcConfig{public static boolean isEnabled() {return false;}} //only online

interface MaskContext{public static boolean isEnabled() {return false;}} //only online
interface PageContext{public static boolean isEnabled() {return false;}} //only online