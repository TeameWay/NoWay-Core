package com.teameway.nowaycore.common.command.export;

/**
 * 导出格式
 */
public enum ExportFormat {
    /**
     * Java 语法片段，落地为 .txt
     */
    JAVA("txt"),
    /**
     * anvillib 数据包定义 JSON，落地为 .json
     */
    JSON("json");

    private final String extension;

    ExportFormat(String extension) {
        this.extension = extension;
    }

    /**
     * @return 文件扩展名，不含点
     */
    public String extension() {
        return this.extension;
    }
}
