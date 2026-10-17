package org.nfcresearch.qpolllab;

/**
 * Interprets the Android system NFC vendor-command result.
 *
 * A return value of 3 is a failure to submit/execute the vendor command,
 * NOT proof that the target e-paper antenna or tag is broken.
 * Keep this independent of Android framework classes for host-side tests.
 */
public final class QTagDiagnosis {
    private QTagDiagnosis() {}

    /**
     * An NCI control header reserves only the low six OID bits (mask 0x3F).
     * Some OEM extensions may use an out-of-band representation, so reject
     * assumptions rather than guessing an alternative opcode.
     */
    public static boolean isStandardNciOid(int oid) {
        return oid >= 0 && oid <= 0x3f;
    }

    public static String wireFormatWarning(int oid) {
        if (isStandardNciOid(oid)) return "";
        return "当前实验命令使用 OID=0x" + Integer.toHexString(oid).toUpperCase(java.util.Locale.ROOT)
            + "，但 NCI 标准命令头 OID 只有 6 位（0x00–0x3F）。"
            + "可能是接口用法或私有扩展封装有误；未核对本机 HAL 前禁止猜测替换为其他 OID。";
    }

    public static boolean accepted(int status) {
        return status == 0;
    }

    public static String statusLabel(int status) {
        switch (status) {
            case 0: return "SUCCESS";
            case 1: return "REJECTED";
            case 2: return "MESSAGE_CORRUPTED";
            case 3: return "FAILED";
            default: return "UNKNOWN(" + status + ")";
        }
    }

    public static String enableFailure(int status) {
        String common = "本轮没有进入特殊寻卡，也没有访问墨水屏；不能据此判定线圈、FMSC 主板或屏幕损坏。";
        switch (status) {
            case 1:
                return "系统/NFC 服务拒绝了 QTag 厂商请求（REJECTED）。"
                    + "请核对系统权限和厂商 NFC 实现。" + common;
            case 2:
                return "厂商 NCI 消息格式/校验被报告异常（MESSAGE_CORRUPTED）。"
                    + "当前 QTag 指令不一定适用于该手机固件。" + common;
            case 3:
                return "厂商 NCI 命令执行失败（FAILED）。NXP 固件名称或隐藏 API 存在"
                    + "不代表该 ROM 支持 QTag/Q-Poll；可能是系统服务、HAL、"
                    + "芯片固件不支持或未能正确执行命令。" + common;
            default:
                return "收到未知厂商 NCI 状态 " + status + "，不能继续寻卡。" + common;
        }
    }

    public static String cleanupFailure(boolean enableAccepted, int disableStatus) {
        String before = enableAccepted
            ? "QTag 曾报告启用成功"
            : "QTag 未报告启用成功";
        return before + "，关闭请求返回 " + statusLabel(disableStatus)
            + " (" + disableStatus + ")；无法确认 NFC 厂商模式是否恢复。"
            + "建议关闭再开启手机 NFC，若异常则重启手机。";
    }

    /** Never overwrite the original failure with a secondary cleanup failure. */
    public static int exitCode(int originalExit, boolean cleanupConfirmed) {
        if (originalExit != 0) return originalExit;
        return cleanupConfirmed ? 0 : 2;
    }
}
