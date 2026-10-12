# 验证记录 — 2026-10-12

- Java 17 运行时；当前环境没有 javac。源码使用 Eclipse ECJ 3.40.0 的 --release 17 编译，生成 Java 17 字节码。
- JUnit Platform Console 1.11.4：34 项测试，34 通过、0 失败、0 跳过。完整 XML 与输出见 verification。
- Maven 项目文件与固定依赖版本已提供；本环境没有完成 Maven 生命周期构建。IntelliJ/JDK 17 可使用 Maven；本次没有把 Maven 构建描述为通过。
- 可执行 JAR 的 ZIP 完整性检查通过；实际运行 import-pine、stage-grass、validate，均通过。错误输入实际退出码为1。
- 合成包：4单位、8观测，格式与关系校验通过。
- PC_B001：280盆单位、1400观测，包括1120个数值终点/缺失单元和280个既有菌根类别。逐个比对原工作簿值与缺失保留。
- PC_B002：1176条收获记录、196个地块；24条没有品种信息的占位行排除。此表包含豆科，未冒充论文的六草分析子集。
- 原始文件在测试与交付版 JAR 导入之后仍与仓库 MD5 完全一致；导入器使用只读工作簿。
- PC_B001 841条设计警告：280未知日历起点、280未知推断簇、280缺少实际暴露历史、1条设计评审提示。所有包的 designReviewed/analysisReady 均 NOT_ASSESSED。
- 当前测试验证数据标准与导入质量；不验证因果估计偏差，也不提供真实因果效应真值。
- Java在线下载入口已实现；本次网络下载使用独立下载流程并核对仓库校验和，未声称在当前环境完成Java在线抓取测试。

## Desktop 0.2.0 验证

- 42项测试全部通过：原34项核心测试与8项桌面/文件工作流测试。
- 测试从界面点击加载公开示例，确认280行单位表展示，再点击Validate得到841项已知设计警告。
- 检查协议保存保留extensions，非法协议不覆盖原文件、非法另存不创建项目目录；已有文件夹不被覆盖。
- 报告包含输入SHA-256；输入变化后拒绝导出旧报告。预览行数上限不影响完整核心校验。
- Swing组件完成无显示环境渲染检查。当前环境无原生显示会话，未声称完成Windows/macOS窗口交互测试。
- 原核心版本的GitHub Actions Maven verify及JAR运行已成功： https://github.com/omixcore/PlantCausal/actions/runs/38253562902 。桌面初版的 Maven 构建、测试和 JAR 导入也已通过：https://github.com/omixcore/PlantCausal/actions/runs/38256013681 。

- 协议表单支持策略定义、实验层级、时间单位、随访、终点和estimand；界面测试确认数值字段编辑仍保留JSON数值类型，以及其他扩展字段。
