package org.opsli.springbootespage.es.base.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 分词器 类型
 *
 * @author WULEI
 * @date 2022年2月18日17:12:19
 */
@AllArgsConstructor
@Getter
public enum AnalyzerType {

    /**
     * 不分词
     */
    NO(""),

    /**
     * 标准分词，默认分词器
     */
    STANDARD("standard"),

    /**
     * ik_smart：会做最粗粒度的拆分；已被分出的词语将不会再次被其它词语占有
     */
    IK_SMART("ik_smart"),

    /**
     * ik_max_word ：会将文本做最细粒度的拆分；尽可能多的拆分出词语
     */
    IK_MAX_WORD("ik_max_word");

    /** 类型 */
    private String type;

}
