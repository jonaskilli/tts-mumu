#!/bin/bash
echo "=== 构建进度检查 ==="
echo "进程状态:"
ps aux | grep -E "gradle|kotlin" | grep -v grep | wc -l
echo ""
echo "APK 输出:"
find /workspace/app/build -name "*.apk" 2>/dev/null || echo "尚无 APK"
echo ""
echo "Class 文件数量:"
find /workspace/app/build -name "*.class" 2>/dev/null | wc -l
echo ""
echo "最近修改:"
ls -lt /workspace/app/build 2>/dev/null | head -5
