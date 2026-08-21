package com.nageoffer.shortlink.project.toolkit;

import cn.hutool.core.util.StrUtil;
import com.nageoffer.shortlink.project.common.convention.exception.ClientException;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 自定义短链校验器
 *
 * <p>校验规则：</p>
 * <ul>
 *     <li>长度 6 ~ 8 位（与表字段 short_uri varchar(8) 的硬上限保持一致）</li>
 *     <li>字符集仅限数字和大小写字母（[0-9A-Za-z]，即 Base62 字符集）</li>
 *     <li>不能以保留字开头（page / notfound / api 等，防止与内部路由冲突）</li>
 * </ul>
 */
public final class ShortUriValidator {

    /**
     * 自定义短链最小长度
     */
    public static final int MIN_LENGTH = 6;

    /**
     * 自定义短链最大长度（对应 t_link.short_uri varchar(8)）
     */
    public static final int MAX_LENGTH = 8;

    /**
     * 合法字符集正则：仅数字和大小写字母
     */
    private static final Pattern URI_PATTERN = Pattern.compile("^[0-9A-Za-z]{" + MIN_LENGTH + "," + MAX_LENGTH + "}$");

    /**
     * 保留字前缀黑名单（大小写不敏感），可按需扩展
     */
    private static final Set<String> RESERVED_PREFIXES = Set.of(
            "page", "notfound", "api", "admin", "login", "static", "assets", "favicon"
    );

    private ShortUriValidator() {
    }

    /**
     * 校验自定义短链，非法时抛出 {@link ClientException}
     *
     * @param customShortUri 用户传入的自定义短链
     */
    public static void validate(String customShortUri) {
        if (StrUtil.isBlank(customShortUri)) {
            throw new ClientException("自定义短链不能为空");
        }
        if (customShortUri.length() < MIN_LENGTH) {
            throw new ClientException(String.format("自定义短链长度不能小于 %d 位", MIN_LENGTH));
        }
        if (customShortUri.length() > MAX_LENGTH) {
            throw new ClientException(String.format("自定义短链长度不能超过 %d 位", MAX_LENGTH));
        }
        if (!URI_PATTERN.matcher(customShortUri).matches()) {
            throw new ClientException("自定义短链仅支持数字和大小写字母（0-9、A-Z、a-z）");
        }
        String lowerUri = customShortUri.toLowerCase(Locale.ROOT);
        for (String reserved : RESERVED_PREFIXES) {
            if (lowerUri.startsWith(reserved)) {
                throw new ClientException(String.format("自定义短链不能以保留字 %s 开头", reserved));
            }
        }
    }
}