package org.dromara.northstar.common.model;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.dromara.northstar.common.SettingOptionsProvider;
import org.dromara.northstar.common.constant.FieldType;

/**
 * 配置项定义
 * @author KevinHuangwl
 *
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface Setting {

	/**
	 * 前端显示的label
	 * @return
	 */
	String label() default "";
	/**
	 * 数值单位
	 * @return
	 */
	String unit() default "";
	/**
	 * 配置项顺序
	 * @return
	 */
	int order() default 0;
	/**
	 * 配置项可选项label
	 * @return
	 */
	String[] options() default {};
	/**
	 * 配置项可选项value
	 * @return
	 */
	String[] optionsVal() default {};
	/**
	 * 配置项实现类（此接口的实现类提供的值将覆盖options与optionsVal的值）
	 * @return
	 */
	Class<? extends SettingOptionsProvider> optionProvider() default SettingOptionsProvider.class;
	/**
	 * 占位文字
	 * @return
	 */
	String placeholder() default "";
	/**
	 * 配置项类型
	 * @return
	 */
	FieldType type() default FieldType.TEXT;
	/**
	 * 是否必填
	 * @return
	 */
	boolean required() default true;
}
