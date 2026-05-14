package com.javaee.documentservice.util;

import com.javaee.documentservice.vo.DocumentCompareVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本差异对比工具类
 * 基于LCS（最长公共子序列）算法实现
 */
public class DiffUtil {

    private static final Logger log = LoggerFactory.getLogger(DiffUtil.class);

    public static DocumentCompareVO compareTexts(String originalText, String revisedText) {
        DocumentCompareVO result = new DocumentCompareVO();
        List<DocumentCompareVO.ChangeDetail> changes = new ArrayList<>();
        int addedLines = 0;
        int removedLines = 0;
        int modifiedLines = 0;

        try {
            if (originalText == null) originalText = "";
            if (revisedText == null) revisedText = "";

            String[] originalLines = originalText.split("\\r?\\n");
            String[] revisedLines = revisedText.split("\\r?\\n");

            // 处理空字符串split返回[""]的情况
            if (originalLines.length == 1 && originalLines[0].isEmpty()) {
                originalLines = new String[0];
            }
            if (revisedLines.length == 1 && revisedLines[0].isEmpty()) {
                revisedLines = new String[0];
            }

            int m = originalLines.length;
            int n = revisedLines.length;
            int[][] dp = new int[m + 1][n + 1];

            for (int i = 1; i <= m; i++) {
                for (int j = 1; j <= n; j++) {
                    if (originalLines[i - 1].equals(revisedLines[j - 1])) {
                        dp[i][j] = dp[i - 1][j - 1] + 1;
                    } else {
                        dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                    }
                }
            }

            int i = m, j = n;
            while (i > 0 || j > 0) {
                if (i > 0 && j > 0 && originalLines[i - 1].equals(revisedLines[j - 1])) {
                    i--;
                    j--;
                } else if (j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])) {
                    DocumentCompareVO.ChangeDetail change = new DocumentCompareVO.ChangeDetail();
                    change.setType("added");
                    change.setLineNumber(j);
                    change.setRevisedText(revisedLines[j - 1]);
                    change.setDescription("新增内容");
                    changes.add(0, change);
                    addedLines++;
                    j--;
                } else if (i > 0) {
                    DocumentCompareVO.ChangeDetail change = new DocumentCompareVO.ChangeDetail();
                    change.setType("removed");
                    change.setLineNumber(i);
                    change.setOriginalText(originalLines[i - 1]);
                    change.setDescription("删除内容");
                    changes.add(0, change);
                    removedLines++;
                    i--;
                }
            }

            result.setChanges(changes);
            result.setAddedLines(addedLines);
            result.setRemovedLines(removedLines);
            result.setModifiedLines(modifiedLines);

            StringBuilder summary = new StringBuilder();
            summary.append("共新增 ").append(addedLines).append(" 行，删除 ").append(removedLines).append(" 行");
            if (modifiedLines > 0) {
                summary.append("，修改 ").append(modifiedLines).append(" 行");
            }
            result.setSummary(summary.toString());

        } catch (Exception e) {
            log.error("文本对比失败", e);
            result.setSummary("对比失败: " + e.getMessage());
        }

        return result;
    }
}
