package org.nfcresearch.qpolllab;

public final class QTagDiagnosisTest {
    private static void check(boolean condition) {
        if (!condition) throw new AssertionError("QTag diagnosis mismatch");
    }

    public static void main(String[] args) {
        check(QTagDiagnosis.isStandardNciOid(0));
        check(QTagDiagnosis.isStandardNciOid(0x3f));
        check(!QTagDiagnosis.isStandardNciOid(-1));
        check(!QTagDiagnosis.isStandardNciOid(0x70));
        check(QTagDiagnosis.wireFormatWarning(0x70).contains("6 位"));
        check(QTagDiagnosis.accepted(0));
        check(!QTagDiagnosis.accepted(1));
        check(!QTagDiagnosis.accepted(2));
        check(!QTagDiagnosis.accepted(3));
        check("SUCCESS".equals(QTagDiagnosis.statusLabel(0)));
        check("REJECTED".equals(QTagDiagnosis.statusLabel(1)));
        check("MESSAGE_CORRUPTED".equals(QTagDiagnosis.statusLabel(2)));
        check("FAILED".equals(QTagDiagnosis.statusLabel(3)));
        check("UNKNOWN(8)".equals(QTagDiagnosis.statusLabel(8)));
        for (int status : new int[] {1, 2, 3, 8}) {
            String explanation = QTagDiagnosis.enableFailure(status);
            check(explanation.contains("没有进入特殊寻卡"));
            check(explanation.contains("不能据此判定线圈"));
        }
        check(QTagDiagnosis.cleanupFailure(false, 3).contains("未报告启用成功"));
        check(QTagDiagnosis.cleanupFailure(true, 3).contains("曾报告启用成功"));
        check(QTagDiagnosis.exitCode(1, false) == 1);
        check(QTagDiagnosis.exitCode(0, false) == 2);
        check(QTagDiagnosis.exitCode(0, true) == 0);
        System.out.println("QTag diagnosis tests passed");
    }
}
