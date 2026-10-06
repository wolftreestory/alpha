package com.alpha.base;

import com.alpha.base.support.AbstractMetaOnline;

public final class BaseUtil extends AbstractMetaOnline {} 

interface MaskContext{public static boolean isEnabled() {return true;}}
interface PageContext{public static boolean isEnabled() {return true;}}

interface BatchConfig{public static boolean isEnabled() {return true;}}

interface MessageSourceConfig{public static boolean isEnabled() {return true;}}
interface JasyptConfig{public static boolean isEnabled() {return true;}}
interface PropertiesConfig{public static boolean isEnabled() {return true;}}
interface SwaggerConfig{public static boolean isEnabled() {return true;}}
interface WebMvcConfig{public static boolean isEnabled() {return true;}}

